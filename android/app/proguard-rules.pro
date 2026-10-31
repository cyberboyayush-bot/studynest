# Retrofit reads generic return types (e.g. Response<Unit>, SubjectsResponse) of suspend functions via
# reflection. R8 full mode strips those signatures unless these types are kept.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation interface me.aysh.studynest.data.remote.StudyNestApi
