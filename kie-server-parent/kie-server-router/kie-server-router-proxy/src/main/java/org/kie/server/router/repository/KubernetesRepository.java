package org.kie.server.router.repository;

import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.kie.server.router.Configuration;
import org.kie.server.router.spi.ConfigRepository;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.*;

import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Created by ianwatson on 19/04/2017.
 */
public class KubernetesRepository implements ConfigRepository, Watcher<Service> {


    private String url = "https://127.0.0.1:8443";
    private Configuration configuration;

    @Override
    public void persist(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Configuration load() {

        Config config = new ConfigBuilder().withMasterUrl(url).build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        configuration = new Configuration();
        ServiceList serviceList  = client.services().list();
        List<Service> services = serviceList.getItems();



        for(Service service : services) {
            String containerName = service.getMetadata().getLabels().get("containerName");
            String serviceName = service.getMetadata().getName();

            String clusterIP = service.getSpec().getClusterIP();
            List<ServicePort> ports = service.getSpec().getPorts();
            int port = ports.get(0).getPort();

            System.out.println("Container Name is " + containerName);
            System.out.println("Service Name is " + serviceName);
            System.out.println("Cluster IP is " + clusterIP);
            System.out.println("Ports " + ports.get(0).getPort());

            configuration.addContainerHost(containerName, "http://" + clusterIP + ":" + port);
            configuration.addServerHost(serviceName, "http://" + clusterIP + ":" + port);
        }

        client.services().watch(this);

        return configuration;
    }

    @Override
    public void clean() {

    }

    @Override
    public void eventReceived(Action action, Service service) {
        try {
            System.out.println("Thread ID -> " + Thread.currentThread().getId());
            System.out.println("Service " + service.getMetadata().getName() + " has been " + action.toString());

            String containerName = service.getMetadata().getLabels().get("containerName");
            String serviceName = service.getMetadata().getName();

            String clusterIP = service.getSpec().getClusterIP();
            List<ServicePort> ports = service.getSpec().getPorts();
            int port = ports.get(0).getPort();

            if (action.equals(Action.ADDED)) {
                System.out.println("Adding container host");
                configuration.addContainerHost(containerName, "http://" + clusterIP + ":" + port);
                System.out.println("Adding server host");
                configuration.addServerHost(serviceName, "http://" + clusterIP + ":" + port);

            } else if (action.equals(Action.DELETED)) {
                configuration.removeContainerHost(containerName, "http://" + clusterIP + ":" + port);
                configuration.removeServerHost(serviceName, "http://" + clusterIP + ":" + port);
            }
            System.out.println("Finished");
        } catch (Exception e) {
            System.out.println("Error has occured!" + e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(KubernetesClientException e) {

    }
}
