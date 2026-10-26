package dev.lioracode.nextbill

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/*
 * JVM 모듈(core:model·core:domain)의 NewApi 검사.
 *
 * Android Lint의 ApiDetector는 모듈별(partial) 분석에서 분석 중인 프로젝트가 Android 프로젝트가 아니면
 * 검사하지 않습니다(`Project.isAndroidProject`가 false면 handler가 null). 그래서 JVM 모듈에
 * `com.android.lint`를 붙이고 app에서 `checkDependencies = true`를 켜도 `LocalDate.ofInstant`(API 34) 같은
 * 호출이 걸리지 않습니다. 대신 컴파일된 클래스의 상수 풀에서 플랫폼 클래스·메서드·필드 참조를 읽어
 * SDK의 `api-versions.xml`과 minSdk로 비교합니다.
 */

/** 클래스 파일이 참조하는 것. [member]가 null이면 클래스 참조, 메서드는 `name(desc)ret`, 필드는 이름. */
data class ApiRef(val owner: String, val member: String?)

/** `api-versions.xml`에서 읽은 클래스별 도입 API와 멤버별 도입 API. */
class ApiDatabase internal constructor(private val classes: Map<String, ApiClass>) {
    internal class ApiClass(val since: Int, val supers: List<String>, val members: Map<String, Int>)

    /** 이 DB가 아는 플랫폼 클래스인지. 앱·라이브러리 클래스는 모릅니다. */
    fun knows(owner: String): Boolean = owner in classes

    fun classSince(owner: String): Int? = classes[owner]?.since

    /** 멤버를 [owner]와 상위 타입에서 찾습니다. 못 찾으면 null(검사하지 않음). */
    fun memberSince(owner: String, member: String, seen: MutableSet<String> = HashSet()): Int? {
        if (!seen.add(owner)) return null
        val c = classes[owner] ?: return null
        c.members[member]?.let { return it }
        return c.supers.firstNotNullOfOrNull { memberSince(it, member, seen) }
    }

    companion object {
        fun parse(input: InputStream): ApiDatabase {
            val result = HashMap<String, ApiClass>()
            val handler = object : DefaultHandler() {
                private var name: String? = null
                private var since = 1
                private var supers = mutableListOf<String>()
                private var members = HashMap<String, Int>()

                override fun startElement(
                    uri: String?,
                    localName: String?,
                    qName: String,
                    attrs: Attributes
                ) {
                    when (qName) {
                        "class" -> {
                            name = attrs.getValue("name")
                            since = apiLevel(attrs.getValue("since")) ?: 1
                            supers = mutableListOf()
                            members = HashMap()
                        }

                        "extends", "implements" -> attrs.getValue("name")?.let { supers += it }

                        "method", "field" -> {
                            // 지워진 멤버도 도입 API는 같으므로 그대로 둡니다.
                            members[attrs.getValue("name")] =
                                apiLevel(attrs.getValue("since")) ?: since
                        }
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String) {
                    if (qName == "class") {
                        name?.let { result[it] = ApiClass(since, supers, members) }
                        name = null
                    }
                }
            }
            SAXParserFactory.newInstance().newSAXParser().parse(input, handler)
            return ApiDatabase(result)
        }

        /** "34", "36.1", "37.0" → 주 버전. */
        private fun apiLevel(value: String?): Int? =
            value?.substringBefore('.')?.substringBefore(':')?.toIntOrNull()
    }
}

/**
 * D8이 minSdk와 관계없이 바꿔 주는 참조(람다·문자열 연결 invokedynamic의 bootstrap, record 등)는
 * 기기에서 직접 불리지 않으므로 검사하지 않습니다.
 */
private val DESUGARED_PREFIXES = listOf("java/lang/invoke/", "java/lang/runtime/")
private val DESUGARED_CLASSES = setOf("java/lang/Record")

/** `java/`·`javax/` 아래지만 플랫폼이 아니라 앱이 함께 싣는 라이브러리(Dagger의 javax.inject). */
private val LIBRARY_PREFIXES = listOf("javax/inject/")

/**
 * D8이 minSdk보다 높은 API의 정적 메서드를 앱 안 코드로 바꿔 주는(backport) 것만 허용합니다. 이름 단위이며
 * 필요한 것만 둡니다. 여기에 없으면 D8이 바꿔 주더라도 실패로 보고합니다.
 */
val D8_BACKPORTS: Map<String, Set<String>> = mapOf(
    "java/util/List" to setOf("of", "copyOf"),
    "java/util/Set" to setOf("of", "copyOf"),
    "java/util/Map" to setOf("of", "copyOf", "entry", "ofEntries"),
    "java/util/Objects" to setOf("requireNonNullElse", "requireNonNullElseGet")
)

private fun ApiRef.isBackport(): Boolean =
    member != null && D8_BACKPORTS[owner]?.contains(member.substringBefore('(')) == true

private fun ApiRef.isDesugared(): Boolean =
    owner in DESUGARED_CLASSES || DESUGARED_PREFIXES.any { owner.startsWith(it) }

private fun isPlatformPackage(owner: String): Boolean =
    (owner.startsWith("java/") || owner.startsWith("javax/")) &&
        LIBRARY_PREFIXES.none { owner.startsWith(it) }

/**
 * [refs] 중 기기(minSdk [minSdk])에서 없을 수 있는 참조. 결과 형식:
 * - `owner#member requires API n`: 나중 API.
 * - `owner is not in the Android API`: `java/`·`javax/` 클래스인데 api-versions.xml에 없음.
 * - `owner#member is not in the Android API`: 아는 클래스(상위 타입 포함)에 없는 멤버.
 * 앱·라이브러리 클래스(그 밖의 패키지)는 검사하지 않습니다.
 */
fun apiViolations(refs: Collection<ApiRef>, db: ApiDatabase, minSdk: Int): List<String> = refs
    .asSequence()
    .filterNot { it.isDesugared() || it.isBackport() }
    .mapNotNull { ref ->
        val name = ref.owner.replace('/', '.') + (ref.member?.let { "#$it" } ?: "")
        val classSince = db.classSince(ref.owner)
            ?: return@mapNotNull if (isPlatformPackage(ref.owner)) {
                "${ref.owner.replace('/', '.')} is not in the Android API"
            } else {
                null
            }
        val since = if (ref.member == null) {
            classSince
        } else {
            val memberSince = db.memberSince(ref.owner, ref.member)
                ?: return@mapNotNull "$name is not in the Android API"
            maxOf(classSince, memberSince)
        }
        if (since > minSdk) "$name requires API $since" else null
    }
    .distinct()
    .sorted()
    .toList()

/** 클래스 파일 상수 풀의 클래스·메서드·필드 참조. */
fun classFileRefs(bytes: ByteArray): Set<ApiRef> {
    val input = DataInputStream(ByteArrayInputStream(bytes))
    require(input.readInt() == CLASS_MAGIC) { "not a class file" }
    input.readUnsignedShort() // minor
    input.readUnsignedShort() // major
    val count = input.readUnsignedShort()
    val utf8 = arrayOfNulls<String>(count)
    val classIndex = IntArray(count)
    val nameAndType = arrayOfNulls<IntArray>(count)
    // tag, 첫째 인덱스, 둘째 인덱스
    val memberRefs = mutableListOf<IntArray>()
    var i = 1
    while (i < count) {
        when (val tag = input.readUnsignedByte()) {
            TAG_UTF8 -> utf8[i] = input.readUTF()

            TAG_INTEGER, TAG_FLOAT -> input.readInt()

            TAG_LONG, TAG_DOUBLE -> {
                input.readLong()
                i++
            }

            TAG_CLASS -> classIndex[i] = input.readUnsignedShort()

            TAG_STRING, TAG_METHOD_TYPE, TAG_MODULE, TAG_PACKAGE -> input.readUnsignedShort()

            TAG_FIELDREF, TAG_METHODREF, TAG_INTERFACE_METHODREF ->
                memberRefs += intArrayOf(tag, input.readUnsignedShort(), input.readUnsignedShort())

            TAG_NAME_AND_TYPE ->
                nameAndType[i] = intArrayOf(input.readUnsignedShort(), input.readUnsignedShort())

            TAG_METHOD_HANDLE -> {
                input.readUnsignedByte()
                input.readUnsignedShort()
            }

            TAG_DYNAMIC, TAG_INVOKE_DYNAMIC -> input.readInt()

            else -> error("unknown constant pool tag $tag")
        }
        i++
    }
    fun className(index: Int): String? = utf8[classIndex[index]]?.let(::elementType)
    val refs = LinkedHashSet<ApiRef>()
    classIndex.forEachIndexed { index, nameIndex ->
        if (nameIndex != 0) className(index)?.let { refs += ApiRef(it, null) }
    }
    memberRefs.forEach { (tag, owner, nat) ->
        val ownerName = className(owner) ?: return@forEach
        val (nameIndex, descIndex) = nameAndType[nat] ?: return@forEach
        val name = utf8[nameIndex] ?: return@forEach
        val member = if (tag == TAG_FIELDREF) name else name + utf8[descIndex]
        refs += ApiRef(ownerName, member)
    }
    return refs
}

/** `[[Ljava/time/LocalDate;` → `java/time/LocalDate`. 원시 타입 배열은 null. */
private fun elementType(name: String): String? {
    if (!name.startsWith("[")) return name
    val element = name.trimStart('[')
    return if (element.startsWith("L") && element.endsWith(";")) {
        element.substring(1, element.length - 1)
    } else {
        null
    }
}

private const val CLASS_MAGIC = 0xCAFEBABE.toInt()
private const val TAG_UTF8 = 1
private const val TAG_INTEGER = 3
private const val TAG_FLOAT = 4
private const val TAG_LONG = 5
private const val TAG_DOUBLE = 6
private const val TAG_CLASS = 7
private const val TAG_STRING = 8
private const val TAG_FIELDREF = 9
private const val TAG_METHODREF = 10
private const val TAG_INTERFACE_METHODREF = 11
private const val TAG_NAME_AND_TYPE = 12
private const val TAG_METHOD_HANDLE = 15
private const val TAG_METHOD_TYPE = 16
private const val TAG_DYNAMIC = 17
private const val TAG_INVOKE_DYNAMIC = 18
private const val TAG_MODULE = 19
private const val TAG_PACKAGE = 20

@CacheableTask
abstract class CheckAndroidApiTask : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classes: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apiVersions: RegularFileProperty

    @get:Input
    abstract val minSdk: Property<Int>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val db = apiVersions.get().asFile.inputStream().buffered().use(ApiDatabase::parse)
        val found = classes.asFileTree.matching { include("**/*.class") }.files
            .sortedBy { it.path }
            .flatMap { file ->
                apiViolations(classFileRefs(file.readBytes()), db, minSdk.get())
                    .map { "${file.name}: $it (current min is ${minSdk.get()})" }
            }
        val out = report.get().asFile
        out.parentFile.mkdirs()
        out.writeText(found.joinToString("\n", postfix = if (found.isEmpty()) "" else "\n"))
        if (found.isNotEmpty()) {
            throw GradleException(
                "NewApi: JVM module code uses APIs missing on minSdk ${minSdk.get()}:\n" +
                    found.joinToString("\n") { "  $it" } +
                    "\nAllowed D8 backports (AndroidApiCheck.kt D8_BACKPORTS): " +
                    D8_BACKPORTS.entries.joinToString { (o, m) -> "${o.replace('/', '.')}#$m" }
            )
        }
    }
}

/**
 * `checkAndroidApi`를 등록하고 이 모듈의 lint 분석(`lintAnalyzeJvmMain`, app의 `lintDebug`가
 * `checkDependencies`로 부름)과 `lint`·`check`에 붙입니다.
 */
internal fun Project.configureAndroidApiCheck() {
    val sdkDir = providers.environmentVariable("ANDROID_HOME")
        .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
        .orElse(
            providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
                .asText.map { text ->
                    text.lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.startsWith("sdk.dir=") }
                        ?.substringAfter("sdk.dir=")
                        .orEmpty()
                }
        )
    val apiFile = sdkDir.map { dir ->
        val candidates = listOf("android-${AndroidSdk.COMPILE}", "android-${AndroidSdk.COMPILE}.0")
            .map { File(dir, "platforms/$it/data/api-versions.xml") }
        candidates.firstOrNull { it.isFile }
            ?: throw GradleException("api-versions.xml not found; tried $candidates")
    }
    val mainClasses = extensions.getByType<SourceSetContainer>().getByName(
        "main"
    ).output.classesDirs
    val task = tasks.register<CheckAndroidApiTask>("checkAndroidApi") {
        group = "verification"
        description =
            "Fails when main classes use Android APIs newer than minSdk ${AndroidSdk.MIN}."
        classes.from(mainClasses)
        apiVersions.fileProvider(apiFile)
        minSdk.set(AndroidSdk.MIN)
        report.set(layout.buildDirectory.file("reports/android-api/main.txt"))
    }
    tasks.matching { it.name == "lintAnalyzeJvmMain" || it.name == "lint" || it.name == "check" }
        .configureEach { dependsOn(task) }
}
