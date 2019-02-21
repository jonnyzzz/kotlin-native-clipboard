
plugins {
  id("org.jetbrains.kotlin.multiplatform") version "1.3.21"
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}


repositories {
  mavenCentral()
}


kotlin {
  macosX64 {
    binaries {
      executable {
        entryPoint = "org.jonnyzzz.kotlin.mpp.template.main"
      }
    }
  }
}
