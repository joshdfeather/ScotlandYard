package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.*;
import java.util.*;

public class Distances {

    void writeFile(List<List<Integer>> distances) {
        FileOutputStream fos;
        ObjectOutputStream oos;
        try {
            fos = new FileOutputStream("t.tmp");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(distances);
            oos.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<List<Integer>> readFile() {
        // Read list of all distances from file t.tmp
        FileInputStream fis;
        ObjectInputStream ois;
        List<List<Integer>> distances;
        try {
            fis = new FileInputStream("cw-ai/src/main/java/uk/ac/bris/cs/scotlandyard/ui/ai/t.tmp");
            ois = new ObjectInputStream(fis);
            distances = (List<List<Integer>>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return distances;
    }

    List<List<Integer>> allDistances(ImmutableValueGraph graph){
        List<List<Integer>> distances = new ArrayList<>();
        for (Object n : graph.nodes()) {
            List<Integer> nodeDistances = dijkstras(graph, (Integer) n);
            distances.add(nodeDistances);
        }
        return distances;
    }


    public List<Integer> dijkstras(
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
            Integer source) {
        // Initialise distances, visited and priority queue
        List<Integer> distances = new ArrayList<>(Collections.nCopies(graph.nodes().size() + 1, 99999));
        distances.set(source, 0);
        Set<Integer> visited = new HashSet<>();
        IndexMinPQ pq = new IndexMinPQ<Integer>(graph.nodes().size() + 1);
        pq.insert(source, 0);

        while (!pq.isEmpty()) {
            // Pull node from top of Priority Queue
            Integer minValue = (Integer) pq.minKey();
            int node = pq.delMin();
            // Visit node
            visited.add(node);
            if (distances.get(node) < minValue) continue;
            // Loop over non visited adjacent nodes
            for (int adj : graph.adjacentNodes(node)) {
                if (visited.contains(adj)) continue;
                // If new distance is shorter update distances and Priority Queue
                Integer newDist = distances.get(node) + 1;
                if (newDist < distances.get(adj)) {
                    distances.set(adj, newDist);
                    if (!pq.contains(adj)) pq.insert(adj, newDist);
                    else pq.decreaseKey(adj, newDist);
                }
            }
        }
        return distances;
    }
}
