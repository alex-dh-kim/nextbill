import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.ApiDatabase
import dev.lioracode.nextbill.ApiRef
import dev.lioracode.nextbill.apiViolations
import dev.lioracode.nextbill.classFileRefs
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assume.assumeTrue
import org.junit.Test

/** 검사 대상 예: API 34의 `LocalDate.ofInstant`와 API 26에도 있는 `atZone`. */
object NewApiProbe {
    fun newApi(i: Instant, z: ZoneId): LocalDate = LocalDate.ofInstant(i, z)

    fun oldApi(i: Instant, z: ZoneId): LocalDate = i.atZone(z).toLocalDate()

    fun field(): LocalDate = LocalDate.MAX
}

class AndroidApiCheckTest {
    private val db = ApiDatabase.parse(
        """
        <api version="4">
          <class name="java/lang/Object" since="1"><method name="&lt;init>()V"/></class>
          <class name="java/time/LocalDate" since="26">
            <extends name="java/lang/Object"/>
            <method name="ofInstant(Ljava/time/Instant;Ljava/time/ZoneId;)Ljava/time/LocalDate;" since="34"/>
            <field name="MAX"/>
          </class>
          <class name="java/time/Instant" since="26">
            <extends name="java/lang/Object"/>
            <method name="atZone(Ljava/time/ZoneId;)Ljava/time/ZonedDateTime;"/>
          </class>
          <class name="java/time/ZonedDateTime" since="26">
            <extends name="java/lang/Object"/>
            <method name="toLocalDate()Ljava/time/LocalDate;"/>
          </class>
          <class name="java/util/SequencedCollection" since="35.0"/>
          <class name="java/util/List" since="1">
            <method name="of(Ljava/lang/Object;)Ljava/util/List;" since="30"/>
            <method name="toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;" since="33"/>
          </class>
          <class name="java/lang/invoke/StringConcatFactory" since="33"/>
        </api>
        """.trimIndent().byteInputStream()
    )

    private fun probeRefs(): Set<ApiRef> {
        val bytes = NewApiProbe::class.java.getResourceAsStream("NewApiProbe.class")!!.readBytes()
        return classFileRefs(bytes)
    }

    @Test
    fun readsMethodReferencesFromClassFile() {
        assertThat(probeRefs()).containsAtLeast(
            ApiRef(
                "java/time/LocalDate",
                "ofInstant(Ljava/time/Instant;Ljava/time/ZoneId;)Ljava/time/LocalDate;"
            ),
            ApiRef("java/time/Instant", "atZone(Ljava/time/ZoneId;)Ljava/time/ZonedDateTime;")
        )
    }

    @Test
    fun readsFieldReferencesFromClassFile() {
        assertThat(probeRefs()).contains(ApiRef("java/time/LocalDate", "MAX"))
    }

    @Test
    fun flagsJdkClassesMissingFromAndroidButNotLibraries() {
        val refs = listOf(
            ApiRef("java/lang/ScopedValue", null),
            ApiRef("javax/naming/Context", "lookup(Ljava/lang/String;)Ljava/lang/Object;"),
            ApiRef("javax/inject/Provider", "get()Ljava/lang/Object;"),
            ApiRef("kotlin/jvm/internal/Intrinsics", "checkNotNull(Ljava/lang/Object;)V")
        )
        assertThat(apiViolations(refs, db, minSdk = 26)).containsExactly(
            "java.lang.ScopedValue is not in the Android API",
            "javax.naming.Context is not in the Android API"
        )
    }

    @Test
    fun flagsUnknownMemberOfKnownClassInsteadOfFallingBackToClassLevel() {
        val refs = listOf(ApiRef("java/time/LocalDate", "ofYearDayInEra(I)Ljava/time/LocalDate;"))
        assertThat(apiViolations(refs, db, minSdk = 26)).containsExactly(
            "java.time.LocalDate#ofYearDayInEra(I)Ljava/time/LocalDate; is not in the Android API"
        )
    }

    @Test
    fun allowsOnlyListedD8Backports() {
        val backport = ApiRef("java/util/List", "of(Ljava/lang/Object;)Ljava/util/List;")
        val notBackported =
            ApiRef("java/util/List", "toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;")
        assertThat(apiViolations(listOf(backport, notBackported), db, minSdk = 26)).containsExactly(
            "java.util.List#toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object; " +
                "requires API 33"
        )
    }

    /** 설치된 SDK의 실제 api-versions.xml로: probe에서 API 34 호출 하나만 걸리고 나머지는 통과합니다. */
    @Test
    fun realSdkDatabaseFlagsOnlyOfInstant() {
        val sdk = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: File(System.getProperty("user.home"), "Library/Android/sdk").path
        val file = File(sdk, "platforms").listFiles().orEmpty()
            .map { File(it, "data/api-versions.xml") }
            .filter { it.isFile }
            .maxByOrNull { it.length() }
        assumeTrue("no api-versions.xml under $sdk", file != null)
        val real = file!!.inputStream().buffered().use(ApiDatabase::parse)

        assertThat(
            real.memberSince(
                "java/time/LocalDate",
                "ofInstant(Ljava/time/Instant;Ljava/time/ZoneId;)Ljava/time/LocalDate;"
            )
        ).isEqualTo(34)
        assertThat(apiViolations(probeRefs(), real, minSdk = 26)).containsExactly(
            "java.time.LocalDate#ofInstant(Ljava/time/Instant;Ljava/time/ZoneId;)" +
                "Ljava/time/LocalDate; requires API 34"
        )
    }

    @Test
    fun flagsMethodNewerThanMinSdk() {
        assertThat(apiViolations(probeRefs(), db, minSdk = 26)).containsExactly(
            "java.time.LocalDate#ofInstant(Ljava/time/Instant;Ljava/time/ZoneId;)" +
                "Ljava/time/LocalDate; requires API 34"
        )
        assertThat(apiViolations(probeRefs(), db, minSdk = 34)).isEmpty()
    }

    @Test
    fun flagsClassesInheritsMembersAndSkipsDesugaredAndUnknown() {
        val refs = listOf(
            ApiRef("java/util/SequencedCollection", null),
            ApiRef("java/time/LocalDate", "MAX"),
            ApiRef("java/time/LocalDate", "<init>()V"),
            ApiRef("java/lang/invoke/StringConcatFactory", null),
            ApiRef("dev/lioracode/Foo", "bar()V")
        )
        assertThat(apiViolations(refs, db, minSdk = 26))
            .containsExactly("java.util.SequencedCollection requires API 35")
        assertThat(db.memberSince("java/time/LocalDate", "<init>()V")).isEqualTo(1)
    }
}
