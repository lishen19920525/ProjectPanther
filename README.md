# ProjectPanther
A concise and quick Android KV database ( based on [SnappyDB](https://github.com/nhachicha/SnappyDB) )

# How to use
### 1. Download aar
Latest release version: [v0.0.1](https://github.com/lishen19920525/ProjectPanther/releases/download/v0.0.1/panther-v0.0.1.aar)
</br>
Put aar file into your project
</br>
And add some dependencies into your gradle
```groovy
  api 'com.snappydb:snappydb-lib:0.5.2'
  api 'com.esotericsoftware.kryo:kryo:2.24.0'
  api 'com.alibaba:fastjson:1.2.40'
```

### 2. Configure the Panther
Implement the PantherModule interface in a class with public visibility
```java
public class DemoPantherModule implements PantherModule {
    @Override
    public PantherConfiguration applyConfiguration(Context context) {
        return new PantherConfiguration.Builder(context)
                .logEnabled(false)
                .databaseName("PantherDemo")
                .build();
    }
}
```
Add your implementation to your list of keeps in your proguard.cfg file
```
-keepnames class com.xxx.xxx.xxx.xxx.DemoPantherModule
-keep class io.panther.**{*;}
```
Add a metadata tag to your AndroidManifest.xml with your PantherModule implementation's fully qualified classname as the value, and "io.panther.PantherModule" as the key
```xml
<meta-data
   android:name="io.panther.PantherModule"
   android:value="com.xxx.xxx.xxx.xxx.DemoPantherModule" />
```
### 3. Enjoy it
Please refer to [Demo](https://github.com/lishen19920525/ProjectPanther/tree/master/app/src/main/java/io/panther/demo) for specific usage
