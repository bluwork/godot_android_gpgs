
app_id = "net.ltslab.games.sud"

def can_build(env, platform):
    return platform == "android"


def configure(env):
    if env["platform"] == "android":
        env.android_add_java_dir("android")
        env.android_add_res_dir("res")
        env.android_add_to_manifest("android/AndroidManifestChunk.xml")
        env.android_add_to_permissions("android/AndroidPermissionsChunk.xml")
        env.android_add_default_config("applicationId '"+app_id+"'")
        env.android_add_dependency("implementation 'com.google.android.gms:play-services-auth:16.0.1'")
        env.android_add_dependency("implementation 'com.google.android.gms:play-services-games:16.0.0'")
        env.disable_module()