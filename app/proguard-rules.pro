# Shizuku instantiates the user service by class name inside its own process.
-keep class io.github.ankitchanki.circlet.shizuku.ShellService {
    public <init>(...);
}

-repackageclasses
-allowaccessmodification
