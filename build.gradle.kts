plugins {
  id("org.jetbrains.kotlin.multiplatform") version "1.3.31"
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}


repositories {
  mavenCentral()
}

val libpng by configurations.creating
val libz   by configurations.creating

dependencies {
  libpng(project(path = ":libpng", configuration = "libpng"))
  libz  (project(path = ":libz", configuration = "zlib"))
}

kotlin {
  macosX64 {
    val main by compilations

    compilations.forEach {
      it.kotlinOptions.freeCompilerArgs += listOf(
              "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
      )
    }

    main.cinterops.create("libpng") {
      packageName = "org.jonnyzzz.png"

      //TODO: setup dependency from the artifact
      includeDirs( Callable { File(libpng.singleFile, "include") } )
    }

    binaries {
      executable {
        linkTask.dependsOn(libz, libpng)

        linkTask.doFirst {

          linkerOpts("-lpng", "-L${File(libpng.singleFile, "lib")}")
          linkerOpts("-L${File(libz.singleFile, "lib")}")
        }
        entryPoint = "org.jonnyzzz.kotlin.mpp.clipboard.main"
      }
    }
  }
}
