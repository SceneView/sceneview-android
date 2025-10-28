package io.github.sceneview

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class TaskWithBinary(binaryName: String) : DefaultTask() {

    @get:Inject
    internal abstract val objects: ObjectFactory

    @get:Inject
    internal abstract val providers: ProviderFactory

    @get:Input
    abstract val binary: Property<String>

    init {
        binary.set(getBinaryPath(binaryName))
    }

    private fun getBinaryPath(binaryName: String): String {
        val filamentToolsDirProvider = project.providers
            .gradleProperty("com.google.android.filament.tools-dir")

        val os = OperatingSystem.current()
        val extension = if (os.isWindows) ".exe" else ""
        val filamentToolsDir = File(filamentToolsDirProvider.get())
        return File(filamentToolsDir, "bin/$binaryName$extension").absolutePath
    }
}

internal open class LogOutputStream(
    private val logger: Logger,
    private val level: LogLevel
) : ByteArrayOutputStream() {
    override fun flush() {
        logger.log(level, toString())
        reset()
    }
}

/**
 * Custom task to compile material files using matc
 * This task handles incremental builds
 */
abstract class MaterialCompiler : TaskWithBinary(binaryName = "matc") {
    @get:Incremental
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    internal abstract val fs: FileSystemOperations

    @get:Inject
    internal abstract val exec: ExecOperations

    @TaskAction
    fun execute(inputs: InputChanges) {
        if (!inputs.isIncremental) {
            fs.delete {
                delete(objects.fileTree().from(outputDir).matching { include("*.filamat") })
            }
        }

        inputs.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val file = change.file

            if (change.changeType == ChangeType.REMOVED) {
                getOutputFile(file).delete()
            } else {
                val out = LogOutputStream(logger, LogLevel.LIFECYCLE)
                val err = LogOutputStream(logger, LogLevel.ERROR)

                val header = "Compiling material $file\n".toByteArray()
                out.write(header)
                out.flush()

                if (!File(binary.get()).exists()) {
                    throw GradleException(
                        "Could not find ${binary.get()}." +
                                " Ensure Filament has been built/installed before building this app."
                    )
                }

                val matcArgs = mutableListOf<String>()
                val excludeVulkan = providers
                    .gradleProperty("com.google.android.filament.exclude-vulkan")
                    .isPresent
                if (!excludeVulkan) {
                    matcArgs += listOf("-a", "vulkan")
                }
                matcArgs += listOf(
                    "-a",
                    "opengl",
                    "-p",
                    "mobile",
                    "-o",
                    getOutputFile(file).toString(),
                    file.toString()
                )

                exec.exec {
                    standardOutput = out
                    errorOutput = err
                    executable = binary.get()
                    args = matcArgs
                }
            }
        }
    }

    private fun getOutputFile(file: File): File {
        return outputDir.file(file.nameWithoutExtension + ".filamat").get().asFile
    }
}

/**
 * Custom task to process IBLs using cmgen
 * This task handles incremental builds
 */
abstract class IblGenerator : TaskWithBinary(binaryName = "cmgen") {

    @get:Input
    @get:Optional
    abstract val cmgenArgs: Property<String>

    @get:Input
    @get:Optional
    abstract val format: Property<String>

    @get:Incremental
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    internal abstract val fs: FileSystemOperations

    @get:Inject
    internal abstract val exec: ExecOperations

    @TaskAction
    fun execute(inputs: InputChanges) {
        if (!inputs.isIncremental) {
            fs.delete {
                delete(objects.fileTree().from(outputDir).matching { include("*") })
            }
        }

        inputs.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val file = change.file

            if (change.changeType == ChangeType.REMOVED) {
                getOutputFile(file).delete()
            } else {
                val out = LogOutputStream(logger, LogLevel.LIFECYCLE)
                val err = LogOutputStream(logger, LogLevel.ERROR)

                val header = "Generating IBL $file\n".toByteArray()
                out.write(header)
                out.flush()

                if (!File(binary.get()).exists()) {
                    throw GradleException(
                        "Could not find ${binary.get()}." +
                                " Ensure Filament has been built/installed before building this app."
                    )
                }

                val outputPath = getOutputFile(file)
                var commandArgs = cmgenArgs.orNull
                if (commandArgs == null) {
                    val format = format.getOrElse("rgb32f")
                    commandArgs =
                        "-q -x $outputPath --format=$format --extract-blur=0.08 --extract=${outputPath.absolutePath}"
                }
                commandArgs = "$commandArgs $file"

                exec.exec {
                    standardOutput = out
                    errorOutput = err
                    executable = binary.get()
                    args = commandArgs.split(" ")
                }
            }
        }
    }

    private fun getOutputFile(file: File): File {
        return outputDir.file(file.nameWithoutExtension).get().asFile
    }
}

/**
 * Custom task to compile mesh files using filamesh
 * This task handles incremental builds
 */
abstract class MeshCompiler : TaskWithBinary(binaryName = "filamesh") {
    @get:Incremental
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    internal abstract val fs: FileSystemOperations

    @get:Inject
    internal abstract val exec: ExecOperations

    @TaskAction
    fun execute(inputs: InputChanges) {
        if (!inputs.isIncremental) {
            fs.delete {
                delete(objects.fileTree().from(outputDir).matching { include("*.filamesh") })
            }
        }

        inputs.getFileChanges(inputFile).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val file = change.file

            if (change.changeType == ChangeType.REMOVED) {
                getOutputFile(file).delete()
            } else {
                val out = LogOutputStream(logger, LogLevel.LIFECYCLE)
                val err = LogOutputStream(logger, LogLevel.ERROR)

                val header = "Compiling mesh $file\n".toByteArray()
                out.write(header)
                out.flush()

                if (!File(binary.get()).exists()) {
                    throw GradleException(
                        "Could not find ${binary.get()}." +
                                " Ensure Filament has been built/installed before building this app."
                    )
                }

                exec.exec {
                    standardOutput = out
                    errorOutput = err
                    executable = binary.get()
                    args = listOf(file.toString(), getOutputFile(file).toString())
                }
            }
        }
    }

    private fun getOutputFile(file: File): File {
        return outputDir.file(file.nameWithoutExtension + ".filamesh").get().asFile
    }
}


/**
 *  This plugin accepts the following parameters:
 *
 *  com.google.android.filament.tools-dir
 *      Path to the Filament distribution/install directory for desktop.
 *      This directory must contain bin/matc.
 *
 *  com.google.android.filament.exclude-vulkan
 *      When set, support for Vulkan will be excluded.
 *
 *  Example:
 *      ./gradlew -Pcom.google.android.filament.tools-dir=../../dist-release assembleDebug
 *
 */
open class FilamentToolsPluginExtension @Inject constructor(objects: ObjectFactory) {
    val materialInputDir: DirectoryProperty = objects.directoryProperty()
    val materialOutputDir: DirectoryProperty = objects.directoryProperty()

    val cmgenArgs: Property<String> = objects.property(String::class.java)
    val iblInputDir: DirectoryProperty = objects.directoryProperty()
    val iblOutputDir: DirectoryProperty = objects.directoryProperty()
    val iblFormat: Property<String> = objects.property(String::class.java)

    val meshInputFile: RegularFileProperty = objects.fileProperty()
    val meshOutputDir: DirectoryProperty = objects.directoryProperty()
}

open class FilamentToolsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                "filamentTools",
                FilamentToolsPluginExtension::class.java,
                project.objects
            )

        val filamentToolsDirProvider = project.providers
            .gradleProperty("com.google.android.filament.tools-dir")

        project.tasks.register("filamentCompileMaterials", MaterialCompiler::class.java) {
            group = "filament"
            enabled = filamentToolsDirProvider.isPresent &&
                    extension.materialInputDir.isPresent &&
                    extension.materialOutputDir.isPresent
            inputDir.set(extension.materialInputDir)
            outputDir.set(extension.materialOutputDir)
        }

        project.tasks.named("preBuild") {
            dependsOn("filamentCompileMaterials")
        }

        project.tasks.register("filamentGenerateIbl", IblGenerator::class.java) {
            group = "filament"
            enabled = filamentToolsDirProvider.isPresent &&
                    extension.iblInputDir.isPresent &&
                    extension.iblOutputDir.isPresent
            cmgenArgs.set(extension.cmgenArgs)
            inputDir.set(extension.iblInputDir)
            outputDir.set(extension.iblOutputDir)
            format.set(extension.iblFormat)
        }

        project.tasks.named("preBuild") {
            dependsOn("filamentGenerateIbl")
        }

        project.tasks.register("filamentCompileMesh", MeshCompiler::class.java) {
            group = "filament"
            enabled = filamentToolsDirProvider.isPresent &&
                    extension.meshInputFile.isPresent &&
                    extension.meshOutputDir.isPresent
            inputFile.set(extension.meshInputFile)
            outputDir.set(extension.meshOutputDir)
        }

        project.tasks.named("preBuild") {
            dependsOn("filamentCompileMesh")
        }
    }
}
