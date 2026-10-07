package com.lovable_clone_microservices.workspace_service.service.impl;

import com.lovable_clone_microservices.workspace_service.dto.deploy.DeployResponse;
import com.lovable_clone_microservices.workspace_service.service.DeploymentService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KubernetesDeploymentServiceImpl implements DeploymentService {

    private final KubernetesClient client;
    private final StringRedisTemplate redisTemplate;
   private ByteArrayOutputStream output = new ByteArrayOutputStream();
   private ByteArrayOutputStream error = new ByteArrayOutputStream();

    private static final String NAMESPACE = "lovable-clone";
    private static final String POOL_LABEL = "status";
    private static final String PROJECT_LABEL = "project-id";
    private static final String IDLE = "idle";
    private static final String BUSY = "busy";
    private static final String SYNCER_CONTAINER = "syncer";
    private static final String RUNNER_CONTAINER = "runner";
    private static final String REVERSE_PROXY_PORT = "8090";

    @Override
    public DeployResponse deploy(Long projectId) {

        String domain = "project-" + projectId + ".app.domain.com";

        Pod existingPod = findActivePod(projectId);

        if(existingPod != null) {
            execCommand(existingPod.getMetadata().getName(), SYNCER_CONTAINER, "mc", "mirror", "--overwrite",
                    String.format("myminio/projectslovable/%d/react-vite-tailwind-daisyui-starter-main/", projectId),
                    "/app/");
            registerRoute(domain, existingPod);
            return new DeployResponse("http://"+domain+":"+REVERSE_PROXY_PORT);
        }

        return claimAndStartNewPod(projectId, domain);
    }

    private DeployResponse claimAndStartNewPod(Long projectId, String domain) {

        Pod pod = client.pods().inNamespace(NAMESPACE)
                .withLabel(POOL_LABEL, IDLE)
                .list().getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No idle runners available. Please scale up the runner-pool."));

        String podName = pod.getMetadata().getName();
        log.info("Claiming pod {} for project {}", podName, projectId);

        client.pods().inNamespace(NAMESPACE).withName(podName).edit(p -> {
            p.getMetadata().getLabels().put(POOL_LABEL, BUSY);
            p.getMetadata().getLabels().put(PROJECT_LABEL, projectId.toString());
            return p;
        });

        try {
            // Syncer Commands
//            String initialSyncCmd = String.format(
//                    "mc mirror --overwrite myminio/projectslovable/%d/react-vite-tailwind-daisyui-starter-main/ /app/",
//                    projectId);
//
//            log.info("Starting initial sync for project {} in pod {}", projectId, podName);
//            execCommand(podName, SYNCER_CONTAINER, "sh", "-c", initialSyncCmd);

            String src = String.format(
                    "myminio/projectslovable/%d/react-vite-tailwind-daisyui-starter-main/", projectId);

            log.info("Starting initial sync for project {} in pod {}", projectId, podName);
            execCommand(podName, SYNCER_CONTAINER, "mc", "mirror", "--overwrite", src, "/app/");

//            String watchCmd = String.format(
//                    "nohup mc mirror --overwrite --watch myminio/projectslovable/%d/react-vite-tailwind-daisyui-starter-main/ /app/ > /app/sync.log 2>&1 &",
//                    projectId);
//            execCommand(podName, SYNCER_CONTAINER, "sh", "-c", watchCmd);

            // Runner Commands
            String installCmd = "cd /app && pnpm install --reporter=append-only";

            log.info("Installing dependencies for project {}...", projectId);
            execCommand(
                    podName,
                    RUNNER_CONTAINER,
                    "sh", "-c", installCmd
            );


//            String startCmd =
//                    "cd /app && nohup npm run dev -- --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &";

            String startCmd = "nohup pnpm run dev --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &";
            log.info("Starting dev server for project {}...", projectId);
            execCommand(
                    podName,
                    RUNNER_CONTAINER,
                    "sh", "-c", startCmd
            );

            registerRoute(domain, pod);

            log.info("Deployment successful: http://{}:{}", domain, REVERSE_PROXY_PORT);
            return new DeployResponse("http://" + domain + ":" + REVERSE_PROXY_PORT);

        } catch(Exception e) {
            log.error("Deployment failed for project {}. Releasing pod {}.", projectId, podName, e);
            client.pods().inNamespace(NAMESPACE).withName(podName).delete();
            throw new RuntimeException("Failed to deploy the project with id: "+projectId);
        }
    }

    private void registerRoute(String domain, Pod pod) {
        String podIp = pod.getStatus().getPodIP();

        if (podIp == null) {
            throw new RuntimeException("Pod is running but has no IP!");
        }

        String key = "route:" + domain;
        String value = podIp + ":5173";

        log.info("Registering route: {} -> {}", key, value);

        redisTemplate.opsForValue().set(
                key,
                value,
                6,
                TimeUnit.HOURS
        );
//        redisTemplate.opsForValue().set(
//                key,
//                value,
//                6,
//                TimeUnit.HOURS
//        );



//        log.info("Redis route write result: {}", result);
        log.info("Redis value seen by Spring immediately after write: {}",
                redisTemplate.opsForValue().get(key));
    }

//    private void execCommand(String podName, String container, String... command) {
//        log.debug("Exec in {}:{} -> {}", podName, container, String.join(" ", command));
//
//        CompletableFuture<String> data = new CompletableFuture<>();
//        try (ExecWatch ignored = client.pods().inNamespace(NAMESPACE).withName(podName)
//                .inContainer(container)
//                .writingOutput(System.out)
//                .writingError(System.err)
//                .usingListener(new ExecListener() {
//                    @Override
//                    public void onClose(int code, String reason) {
//                        data.complete("Done");
//                    }
//                })
//                .exec(command)) {
//
//            if (command[command.length - 1].trim().endsWith("&")) {
//                Thread.sleep(500);
//            } else {
//                data.get(5, TimeUnit.MINUTES);
//            }
//
//            log.info("Exec output: {}", output);
//            log.error("Exec error: {}", error);
//
//        } catch (Exception e) {
//            log.error("Exec failed", e);
//            throw new RuntimeException("Pod Execution Failed", e);
//        }
//    }

    private void execCommand(String podName, String container, String... command) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        try (ExecWatch watch = client.pods().inNamespace(NAMESPACE).withName(podName)
                .inContainer(container)
                .writingOutput(out)
                .writingError(err)
                .exec(command)) {

            int code = watch.exitCode().get(5, TimeUnit.MINUTES);
            log.info("Exec [{}] exit={} out={}", container, code, out);
            if (code != 0) {
                throw new RuntimeException("Exit " + code + ": " + err);
            }
        } catch (Exception e) {
            throw new RuntimeException("Pod Execution Failed: " + e.getMessage(), e);
        }
    }

    Pod findActivePod(Long projectId) {
        return client.pods().inNamespace(NAMESPACE)
                .withLabel(PROJECT_LABEL, projectId.toString())
                .withLabel(POOL_LABEL, BUSY) // Only find active/busy ones
                .list().getItems().stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .findFirst()
                .orElse(null);
    }


}