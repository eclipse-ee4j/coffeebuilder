File buildLog = new File( basedir, "build.log" );

assert buildLog.isFile()
assert buildLog.text.contains("Coffee Builder Maven Plugin")
