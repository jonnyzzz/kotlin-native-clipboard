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
    compilations.forEach {
      it.kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    }

    binaries {
      executable {
        entryPoint = "org.jonnyzzz.kotlin.mpp.clipboard.main"
      }
    }
  }
}
