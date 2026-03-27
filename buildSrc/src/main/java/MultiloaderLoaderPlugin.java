import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.net.URL;
import java.util.Map;

public class MultiloaderLoaderPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("multiloader-common");
        applyScript(project, "multiloader-loader.gradle");
    }

    private static void applyScript(Project project, String resourceName) {
        URL scriptUrl = MultiloaderLoaderPlugin.class.getClassLoader().getResource(resourceName);
        if (scriptUrl == null) {
            throw new IllegalStateException("Missing embedded Gradle script resource: " + resourceName);
        }

        project.apply(Map.of("from", scriptUrl));
    }
}
