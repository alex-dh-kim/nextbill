import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.isCheckedConfiguration
import dev.lioracode.nextbill.violations
import org.junit.Test

class ModuleRulesTest {
    @Test
    fun featureImplMustNotDependOnOtherFeatureImpl() {
        val v = violations(mapOf(":feature:home:impl" to setOf(":feature:editor:impl")))
        assertThat(v).containsExactly(
            ":feature:home:impl -> :feature:editor:impl (feature impl must depend only on other features' api)"
        )
    }

    @Test
    fun domainMayDependOnlyOnModel() {
        assertThat(violations(mapOf(":core:domain" to setOf(":core:model")))).isEmpty()
        assertThat(violations(mapOf(":core:domain" to setOf(":core:database")))).hasSize(1)
    }

    @Test
    fun modelMustHaveNoProjectDependencies() {
        assertThat(violations(mapOf(":core:model" to emptySet()))).isEmpty()
        assertThat(violations(mapOf(":core:model" to setOf(":core:domain")))).hasSize(1)
    }

    @Test
    fun onlyDataMayDependOnDatabaseAndDatastore() {
        assertThat(
            violations(mapOf(":core:data" to setOf(":core:database", ":core:datastore")))
        ).isEmpty()
        assertThat(violations(mapOf(":widget" to setOf(":core:database")))).hasSize(1)
        assertThat(violations(mapOf(":feature:home:impl" to setOf(":core:datastore")))).hasSize(1)
    }

    @Test
    fun featureMustNotDependOnData() {
        assertThat(violations(mapOf(":feature:home:impl" to setOf(":core:data")))).hasSize(1)
        assertThat(violations(mapOf(":feature:home:api" to setOf(":core:data")))).hasSize(1)
    }

    @Test
    fun appMayDependOnData() {
        assertThat(violations(mapOf(":app" to setOf(":core:data", ":feature:home:impl")))).isEmpty()
    }

    @Test
    fun featureImplMayDependOnOtherFeatureApi() {
        assertThat(
            violations(
                mapOf(":feature:home:impl" to setOf(":feature:editor:api", ":feature:detail:api"))
            )
        ).isEmpty()
    }

    @Test
    fun checkedConfigurations() {
        listOf("api", "implementation", "debugImplementation", "compileOnly", "runtimeOnly")
            .forEach { assertThat(isCheckedConfiguration(it)).isTrue() }
        listOf(
            "testImplementation",
            "androidTestImplementation",
            "testDebugImplementation",
            "kspTest"
        )
            .forEach { assertThat(isCheckedConfiguration(it)).isFalse() }
    }
}
