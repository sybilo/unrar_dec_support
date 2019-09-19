name := baseDirectory.value.name

version := "1.0"

scalaVersion := "2.12.6"

//libraryProject := true

unmanagedBase := baseDirectory.value / "lib"

// Java Code 必须用这种方式
javaSource in Compile := baseDirectory.value / "src"

exportJars := true

offline := true

// 解决生成文档报错导致 jitpack.io 出错的问题。
publishArtifact in packageDoc := false

libraryDependencies ++= Seq(
)
