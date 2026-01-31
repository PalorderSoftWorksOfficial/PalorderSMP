# PalorderSMP-tweaked

PalorderSMP-Tweaked is an open-source server-side mod created by PalorderSoftWorksOfficial. Feel free to contact me if you want new features added (but nothing too intense 😉).

> **Note:** If you're looking for the `.jar` without building, you’ll need to compile it yourself—let Gradle handle the hard work and it will generate a functional mod for you.

## How to Build the Project

### Step 1: Clone the Repository
Clone this repository and locate the `gradlew.bat` file. Then, update your `gradle.properties` file with the following content:

```properties
# Sets default memory used for Gradle commands. Can be overridden by user or command line properties.
# Required to provide enough memory for Minecraft decompilation process
org.gradle.jvmargs=-Xmx4g -Xms4g -Xss2m
org.gradle.daemon=true
```

### Step 2: Run Gradle
Run the `gradlew.bat` file. Wait until you see one of these messages on the last line:  
- `Build Completed`  
- `Successfully Executed Build`  
- `Successfully Generated Build`

## How to Get the Mod File (`.jar`)
Once the build completes:  
1. Look in the **project root directory**.  
2. Open the `build` folder, then the `libs` folder inside it.  
3. Inside, you’ll find the generated `.jar` file ready to use with **Forge 1.20.1+**.

## Reporting Issues
If you encounter problems or bugs, report them here: [PalorderSMP Issues Page](https://github.com/PalorderSoftWorksOfficial/PalorderSMP/issues)

## Requirements
- **JDK 17+** installed. Download from [Adoptium](https://adoptium.net/temurin/releases/?version=17)  
- **At least 8GB RAM** on your system (Gradle uses 4GB; the rest is for your system)  
- **Minecraft 1.20.1** compatible Forge loader

## Licensing & Support
- Read the [License](License.md) for terms and usage.  
- You can DM me on Discord for non-copylocked versions or commercial inquiries: **@PalorderCorporation**  
- Repository: [PalorderSMP on GitHub](https://github.com/PalorderSoftWorksOfficial/PalorderSMP)

## Statuses

### Build and release: [![Build and Release Minecraft Mod](https://github.com/PalorderSoftWorksOfficial/PalorderSMP/actions/workflows/release.yml/badge.svg)](https://github.com/PalorderSoftWorksOfficial/PalorderSMP/actions/workflows/release.yml)
