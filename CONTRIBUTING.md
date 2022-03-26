# How to contribute

## Issues

There is no special template for issues. You just need to select a title that characterizes the problem in general and provide a short description of the problem in a comment. You should also include code, logs or screenshots if they help to understand the problem.

Feature requests are welcomed too!

## Discussions

You can create a discussion if you have a question rather than a problem report. You should also copy your questions and provided answers from the Discord server if you feel that other developers can benefit from them in future.

## Pull requests

You can create a pull request if you know how to fix a problem, improve the documentation or implement a new feature. You just need to fork the repository, commit your changes and create a pull request from there. After the changes are merged the Discord bot will award you the **Contributor** role :tada:

### Title

You should start the title of the pull request with an uppercase letter.

### Description

You should provide a short description of your changes so other contributors can better understand them.

### Author

You need to make sure that you use the same name and email as in your GitHub account when committing changes. Otherwise, the Discord bot may have difficulties with awarding you the **Contributor** role.

### Code Style

We use the official [Kotlin style guide](https://developer.android.com/kotlin/style-guide) in the project. The code style is stored in the repository so everything should be configured automatically when you open the project in Android Studio.

### Changes in source code

You should keep changes as minimal as possible, however, you can fix obvious mistakes in the source code, formatting or documentation.

### Changes in Filament materials

You should recompile the Filament materials using the [current Filament version](https://github.com/google/filament/releases) if you make any changes to them. The recommended way to do that is to enable the [Filament plugin](https://github.com/SceneView/sceneview-android/blob/main/gradle.properties) and build the project.
