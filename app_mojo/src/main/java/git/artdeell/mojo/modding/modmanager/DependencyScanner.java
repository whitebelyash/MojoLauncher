package git.artdeell.mojo.modding.modmanager;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.andreasmelone.basicmodinfoparser.platform.BasicModInfo;
import me.andreasmelone.basicmodinfoparser.platform.Platform;
import me.andreasmelone.basicmodinfoparser.platform.dependency.Dependency;
import me.andreasmelone.basicmodinfoparser.util.ModInfoParseException;

public class DependencyScanner {
    private final Platform mPlatform;
    private final Map<String, ModInfo> mProviders = new HashMap<>();
    private final Map<String, List<ModInfo>> mDeps = new HashMap<>();

    public DependencyScanner(Platform platform, List<ModInfo> modInfos) {
        this.mPlatform = platform;
        build(modInfos);
    }

    private BasicModInfo getBasicInfo(ContainedModInfo cmi) {
        try {
            BasicModInfo[] infos = cmi.modFile.getInfo(mPlatform);
            return infos.length > 0 ? infos[0] : null;
        } catch (ModInfoParseException e) {
            Log.e("DependencyScanner", "Unable to parse mod " + cmi.jarFile.getName() + "!");
            return  null;
        }
    }

    private void build(List<ModInfo> modInfos) {
        for(ModInfo mi : modInfos) {
            if(!(mi instanceof ContainedModInfo)) continue;
            BasicModInfo bmi = getBasicInfo((ContainedModInfo) mi);
            if(bmi == null) continue;
            if(bmi.getId() != null) mProviders.put(bmi.getId(), mi);
            for(Dependency dep : bmi.getDependencies()) {
                List<ModInfo> dependants = mDeps.get(dep.getModId());
                if(dependants == null) {
                    dependants = new ArrayList<>();
                    mDeps.put(dep.getModId(), dependants);
                }
                dependants.add(mi);
            }
        }
    }
    private List<Dependency> getDependencies(ModInfo mi) {
        if(!(mi instanceof ContainedModInfo)) return Collections.emptyList();
        BasicModInfo bmi = getBasicInfo((ContainedModInfo) mi);
        return bmi != null ? bmi.getDependencies() : Collections.emptyList();
    }
    public List<ModInfo> getDirectDependencies(ModInfo mi) {
        if(!(mi instanceof ContainedModInfo)) return Collections.emptyList();
        BasicModInfo bmi = getBasicInfo((ContainedModInfo) mi);
        if(bmi == null) return Collections.emptyList();
        List<ModInfo> res = new ArrayList<>();
        for(Dependency dep : bmi.getDependencies()) {
            List<ModInfo> deps = mDeps.get(dep.getModId());
            if(deps != null) res.addAll(deps);
        }
        return res;
    }

    public List<ModInfo> getEnabledDependencies(ModInfo mi) {
        BasicModInfo bmi = getBasicInfo((ContainedModInfo) mi);
        if(bmi == null || bmi.getId() == null) return Collections.emptyList();
        List<ModInfo> res = new ArrayList<>();
        Set<ModInfo> visited = new HashSet<>();
        Deque<ModInfo> queue = new ArrayDeque<>();
        visited.add(mi);
        List<ModInfo> direct = mDeps.get(bmi.getId());
        if(direct != null) queue.addAll(direct);
        while(!queue.isEmpty()) {
            ModInfo current = queue.poll();
            if(current == mi || visited.contains(current)) continue;
            visited.add(current);
            if(current.disabled) continue;
            res.add(current);
            for(Dependency dep : getDependencies(current)) {
                if(!dep.isMandatory()) continue;
                List<ModInfo> next = mDeps.get(dep.getModId());
                if(next != null) queue.addAll(next);
            }
        }
        return res;
    }
}
