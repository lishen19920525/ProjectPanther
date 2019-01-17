# ProjectPanther
A concise and quick Android KV database ( based on [SnappyDB](https://github.com/nhachicha/SnappyDB) )

# How to use
### 1. Implementation
[![](https://jitpack.io/v/lishen19920525/ProjectPanther.svg)](https://jitpack.io/#lishen19920525/ProjectPanther)
Add some dependencies into your gradle
```groovy
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
```groovy
  implementation 'com.snappydb:snappydb-lib:0.5.2'
  implementation 'com.esotericsoftware.kryo:kryo:2.24.0'
  implementation 'com.google.code.gson:gson:2.8.2'
  implementation 'com.github.lishen19920525:ProjectPanther:v1.0.0'
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
