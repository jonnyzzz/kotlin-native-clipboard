import de.undercouch.gradle.tasks.download.Download

plugins {
  id("de.undercouch.download") version "3.4.3"
}

val libpngVersion = "1.6.37"
val libpngURL = "https://download.sourceforge.net/libpng/libpng-$libpngVersion.tar.gz"

val libpngBase = File(buildDir, "libpng")
val libpngSource = File(libpngBase, libpngURL.split("/").last())
val libpngUnpacked = File(libpngBase, libpngURL.split("/").last().removeSuffix(".tar.gz"))
val libpngPrefix = File(libpngBase, libpngURL.split("/").last().removeSuffix(".tar.gz") + "-bin")

//TODO: download only one platform
val download = tasks.create<Download>("libpng_download") {
  inputs.property("url", libpngURL)
  src(libpngURL)
  dest(libpngSource)
  overwrite(false)
}

val unpack = tasks.create<Sync>("libpng_unpack") {
  dependsOn(download)
  from({ tarTree(resources.gzip(libpngSource)) })
  into(libpngUnpacked)
  includeEmptyDirs = false
  eachFile {
    path = path.split("/", limit = 2)[1]
  }

  preserve {
    include(".libs/**")
    include("**/*.o")
  }
}


val zlib by configurations.creating

dependencies {
  zlib(project(path = ":libz", configuration = "zlib"))
}

fun Exec.setupZLibEnvironment() {
  infix fun String.env(value: () -> Any) {
    inputs.property("ENV-$this", value)
    doFirst {
      environment(this@env, value().toString())
    }
  }

  val zlibRoot by lazy { zlib.singleFile }
  fun resolve(name: String) = lazy {
    val item = File(zlibRoot, name)
    if (!item.exists()) {
      error("File $item must exist")
    }
    item
  }

  val lib by resolve("lib")
  val inc by resolve("include")

  "ZLIBINC" env { inc }
  "ZLIBLIB" env { lib }
  "CPPFLAGS" env { "-I$inc -DPNG_SETJMP_NOT_SUPPORTED -mmacosx-version-min=10.11 " }
  "LDFLAGS" env { "-L$lib " }
}

val configure = tasks.create<Exec>("libpng_configure") {
  dependsOn(zlib, unpack)

  setupZLibEnvironment()

  commandLine("/bin/sh", "-c", "./configure --disable-shared --enable-static --prefix=$libpngPrefix --with-zlib-prefix")
  workingDir(libpngUnpacked)
}


val make = tasks.create<Exec>("libpng_make") {
  dependsOn(configure)

  setupZLibEnvironment()
  commandLine("/bin/sh", "-c", "make install prefix=$libpngPrefix")
  workingDir(libpngUnpacked)
}


val libpng by configurations.creating

artifacts.add(libpng.name, libpngPrefix) {
  builtBy(make)
}


