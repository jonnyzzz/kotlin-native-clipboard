plugins {
  id("org.jetbrains.kotlin.multiplatform") version "1.3.21"
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}


repositories {
  mavenCentral()
}

val libpng by configurations.creating

dependencies {
  libpng(project(path = ":libpng", configuration = "libpng"))
}

kotlin {
  macosX64 {
    val main by compilations

    compilations.forEach {
      it.kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    }

    main.cinterops.create("libpng") {
      packageName = "org.jonnyzzz.png"
      
      includeDirs( Callable { File(libpng.singleFile, "include") } )
    }

    binaries {
      executable {
        entryPoint = "org.jonnyzzz.kotlin.mpp.clipboard.main"
      }
    }
  }
}
