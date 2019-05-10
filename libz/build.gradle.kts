import de.undercouch.gradle.tasks.download.Download

plugins {
  id("de.undercouch.download") version "3.4.3"
}

val libzVersion = "1.2.11"
val libzURL = "https://www.zlib.net/zlib-$libzVersion.tar.gz"

val libzBase = File(buildDir, "libz")
val libzSource = File(libzBase, libzURL.split("/").last())
val libzUnpacked = File(libzBase, libzURL.split("/").last().removeSuffix(".tar.gz"))
val libzPrefix = File(libzBase, libzURL.split("/").last().removeSuffix(".tar.gz") + "-bin")

//TODO: download only one platform
val download = tasks.create<Download>("libz_download") {
  src(libzURL)
  dest(libzSource)
  overwrite(false)
}

val unpack = tasks.create<Sync>("libz_unpack") {
  dependsOn(download)
  from({ tarTree(resources.gzip(libzSource)) })
  into(libzUnpacked)
  includeEmptyDirs = false
  eachFile {
    path = path.split("/", limit = 2)[1]
  }
  preserve {
    include(".libs/**")
    include("**/*.o")
  }
}

fun Exec.setupZLibEnvironment() {
  doFirst {
    infix fun String.env(value: Any) {
      environment(this, value.toString())
    }

    "CPPFLAGS" env "-mmacosx-version-min=10.11"
  }
}

val configure = tasks.create<Exec>("libz_configure") {
  dependsOn(unpack)

  setupZLibEnvironment()
  commandLine("/bin/sh", "-c", "./configure --static --prefix=$libzPrefix --archs=\"-arch x86_64\"")
  workingDir(libzUnpacked)
}

val make = tasks.create<Exec>("libz_make") {
  dependsOn(configure)

  setupZLibEnvironment()
  commandLine("/bin/sh", "-c", "make install prefix=$libzPrefix")
  workingDir(libzUnpacked)
}


val zlib by configurations.creating

artifacts.add(zlib.name, libzPrefix) {
  builtBy(make)
}

