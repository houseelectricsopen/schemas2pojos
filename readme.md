# Schemas2Pojos

This project offers a maven plugin for mapping from schemas to pojos

## Building Maven Plugin
<pre>
cd schemas2pojosmvn
#build and compile source
mvn clean install
#compile typescript
. ./compilets.sh
</pre>

## Running Test
<pre>
cd schemas2pojomvntest
mvn clean install
</pre>
This generates code into schemas2pojomvntest/generated-sources
These are configured [here](schemas2pojomvntest/pom.xml)

## Mapping json schema to java
set schemaFormat to __json__, destination format to __java__
this example generates java into __target/generated-sources/java__:
```xml
<project>
....
    <build>
        <plugins>
            <plugin>
                <groupId>com.typedpath</groupId>
                <artifactId>schemas2pojosmvn</artifactId>
                <version>1.0.1</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <id>json2java</id>
                        <goals>
                            <goal>schema2language</goal>
                        </goals>
                        <configuration>
                            <schemaFormat>json</schemaFormat>
                            <destinationFormat>java</destinationFormat>
                            <sourceRoot>${project.basedir}/src/main/resources/samples</sourceRoot>
                            <sourceIncludes>**/*.json</sourceIncludes>
                            <destinationRoot>${project.basedir}/target/generated-sources</destinationRoot>
                            <typeMappings>
                                <typeMapping><from>string</from><to>String</to></typeMapping>
                                <typeMapping><from>integer</from><to>int</to></typeMapping>
                                <typeMapping><from>int</from><to>int</to></typeMapping>
                                <typeMapping><from>boolean</from><to>Boolean</to></typeMapping>
                                <typeMapping><format>dateTime</format><to>java.util.Date</to></typeMapping>
                            </typeMappings>
                        </configuration>
                    </execution>
                </executions>
                <dependencies>
                </dependencies>
            </plugin>
```

## Mapping json schema to typescript
set schemaFormat to __json__, destination format to __typescript__
this example generates typescript into __target/generated-source/typescript__
```xml
<project>
....   
    <build>
        <plugins>
            <plugin>
                <groupId>com.typedpath</groupId>
                <artifactId>schemas2pojosmvn</artifactId>
                <version>1.0.1</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <id>json2Typescript</id>
                        <goals>
                            <goal>schema2language</goal>
                        </goals>
                        <configuration>
                            <schemaFormat>json</schemaFormat>
                            <destinationFormat>typescript</destinationFormat>
                            <sourceRoot>${project.basedir}/src/main/resources/samples</sourceRoot>
                            <sourceIncludes>**/*.json</sourceIncludes>
                            <destinationRoot>${project.basedir}/target/typescript</destinationRoot>
                            <typeMappings>
                                <typeMapping><from>string</from><to>string</to></typeMapping>
                                <typeMapping><from>integer</from><to>number</to></typeMapping>
                                <typeMapping><from>int</from><to>number</to></typeMapping>
                                <typeMapping><from>boolean</from><to>boolean</to></typeMapping>
                                <typeMapping><format>dateTime</format><to>string</to></typeMapping>
                            </typeMappings>
                        </configuration>
                    </execution>
...
```

## Mapping json to kotlin
set schemaFormat to __json__, destination format to __kotlin__
this example generates kotlin into __target/generated-source/kotlin__

```xml
<project>
....   
    <build>
        <plugins>
            <plugin>
                    <execution>
                        <phase>generate-sources</phase>
                        <id>json2kotlin</id>
                        <goals>
                            <goal>schema2language</goal>
                        </goals>
                        <configuration>
                            <schemaFormat>json</schemaFormat>
                            <destinationFormat>kotlin</destinationFormat>
                            <sourceRoot>${project.basedir}/src/main/resources/ksamples</sourceRoot>
                            <sourceIncludes>**/s3CreateObjectEvent.json</sourceIncludes>
                            <destinationRoot>${project.basedir}/target/generated-sources</destinationRoot>
                            <typeMappings>
                                <typeMapping><from>string</from><to>String</to></typeMapping>
                                <typeMapping><from>integer</from><to>kotlin.Int</to></typeMapping>
                                <typeMapping><from>int</from><to>kotlin.Int</to></typeMapping>
                                <typeMapping><from>boolean</from><to>Boolean</to></typeMapping>
                                <typeMapping><format>dateTime</format><to>java.util.Date</to></typeMapping>
                            </typeMappings>
                        </configuration>
                    </execution>
...
```


## Mapping kotlin to typescript - coming soon
(Makes sense for a kotlin backend + typescript front end)

