# Shizuku instantiates the user service by class name inside its own process.
-keep class com.parallelc.micts.shizuku.ShellService {
    public <init>(...);
}

-repackageclasses
-allowaccessmodification
