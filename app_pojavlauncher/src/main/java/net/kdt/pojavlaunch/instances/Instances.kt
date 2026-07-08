package net.kdt.pojavlaunch.instances

import com.google.gson.JsonSyntaxException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.File
import java.io.IOException
import java.util.*

class Instances private constructor(
    @JvmField val list: List<DisplayInstance>,
    @JvmField val selectedIndex: Int
) {
    companion object {
        private val sInstancePath = File(Tools.DIR_GAME_HOME, "instances")
        @JvmField val SHARED_DATA_DIRECTORY = File(Tools.DIR_GAME_HOME, "shared_dir")

        private fun <T : DisplayInstance> read(instanceRoot: File, tClass: Class<T>): T? {
            return try {
                val instance = JSONUtils.readFromFile(metadataLocation(instanceRoot), tClass) ?: return null
                instance.mInstanceRoot = instanceRoot
                instance
            } catch (_: IOException) {
                null
            } catch (_: JsonSyntaxException) {
                null
            }
        }

        @JvmStatic
        fun metadataLocation(instanceDir: File): File {
            return File(instanceDir, "mojo_instance.json")
        }

        private fun selectedInstanceLocation(): File? {
            val directoryName = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_INSTANCE, "")
            val instanceRoot = File(sInstancePath, directoryName)
            if (!metadataLocation(instanceRoot).exists()) return null
            return instanceRoot
        }

        private fun filterInstanceDirectories(instanceDir: File): Boolean {
            if (!instanceDir.canRead() || !instanceDir.canWrite()) return false
            if (!instanceDir.isDirectory) return false
            val instanceMetadata = metadataLocation(instanceDir)
            return instanceMetadata.isFile && instanceMetadata.canRead()
        }

        private fun <T : DisplayInstance> loadInstances(
            tClass: Class<T>,
            selectionDst: IntArray?
        ): List<T> {
            synchronized(sInstancePath) {
                FileUtils.ensureDirectory(sInstancePath)
            }
            val instanceDirectories = sInstancePath.listFiles { file -> filterInstanceDirectories(file) }
                ?: throw IOException("Failed to enumerate instances")
            val selectedInstanceLocation = if (selectionDst != null) selectedInstanceLocation() else null
            val instances = ArrayList<T>(instanceDirectories.size)

            for (instanceDir in instanceDirectories) {
                val instance = read(instanceDir, tClass) ?: continue
                instance.sanitize()
                instances.add(instance)
                if (selectionDst != null && instanceDir == selectedInstanceLocation) {
                    selectionDst[0] = instances.size - 1
                }
            }
            instances.trimToSize()
            return instances
        }

        @Throws(IOException::class)
        fun loadDisplay(): Instances {
            val selectionIndex = intArrayOf(-1)
            val instances = loadInstances(DisplayInstance::class.java, selectionIndex)
            return if (instances.isEmpty()) {
                createFirstTimeInstance()
                loadDisplay()
            } else {
                if (selectionIndex[0] == -1) {
                    setSelectedInstance(instances[0])
                    selectionIndex[0] = 0
                }
                Instances(Collections.unmodifiableList(instances), selectionIndex[0])
            }
        }

        @Throws(IOException::class)
        fun loadAllInstances(): List<Instance> {
            return loadInstances(Instance::class.java, null)
        }

        private fun findNewInstanceRoot(prefix: String?): File {
            var instanceRoot: File
            do {
                var proposedDirectoryName = UUID.randomUUID().toString()
                if (prefix != null) {
                    proposedDirectoryName = "$prefix-$proposedDirectoryName"
                }
                instanceRoot = File(sInstancePath, proposedDirectoryName)
            } while (instanceRoot.exists() && instanceRoot.isDirectory)
            return instanceRoot
        }

        fun setSelectedInstance(instance: DisplayInstance) {
            LauncherPreferences.DEFAULT_PREF.edit()
                .putString(
                    LauncherPreferences.PREF_KEY_CURRENT_INSTANCE,
                    instance.mInstanceRoot?.name
                ).apply()
        }

        @Throws(IOException::class)
        fun removeInstance(instance: Instance) {
            val instanceDirectory = instance.mInstanceRoot ?: return
            org.apache.commons.io.FileUtils.deleteDirectory(instanceDirectory)
        }

        @Throws(IOException::class)
        private fun createFirstTimeInstance() {
            internalCreateInstance({ instance ->
                instance.sharedData = true
                instance.versionId = "1.12.2"
            }, null)
        }

        @Throws(IOException::class)
        fun createDefaultInstance(): Instance {
            return createInstance({ instance ->
                instance.sharedData = true
                instance.versionId = Instance.VERSION_LATEST_RELEASE
            }, null)
        }

        @Throws(IOException::class)
        private fun internalCreateInstance(
            instanceSetter: InstanceSetter,
            namePrefix: String?
        ): Instance {
            val root = findNewInstanceRoot(namePrefix)
            FileUtils.ensureDirectory(root)
            val instance = Instance()
            instance.mInstanceRoot = root
            instanceSetter.setInstanceProperties(instance)
            instance.write()
            return instance
        }

        @Throws(IOException::class)
        fun createInstance(instanceSetter: InstanceSetter, namePrefix: String?): Instance {
            return internalCreateInstance(instanceSetter, namePrefix)
        }

        fun loadSelectedInstance(): Instance? {
            val selectedInstanceLocation = selectedInstanceLocation() ?: return null
            val instance = read(selectedInstanceLocation, Instance::class.java) ?: return null
            instance.sanitize()
            return instance
        }
    }
}
