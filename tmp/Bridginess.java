/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ixxi.bridginess;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import org.gephi.algorithms.shortestpath.AbstractShortestPathAlgorithm;
import org.gephi.algorithms.shortestpath.BellmanFordShortestPathAlgorithm;
import org.gephi.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeData;
import org.gephi.graph.api.NodeIterable;

import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;

/**
 *
 * @author MM@IXXI
 */


public class Bridginess implements Statistics, LongTask {
    
    public static final String BRIDGINESS = "bridginess";
    //public static final String BETWEENNESS = "betweenness";
    
    private int exclNeighbors;
    
    private ProgressTicket progress;
    private boolean isCanceled;
    private int N;
    
    private int shortestPaths;
    private double[] bridginess;
    
    private double avgBridginess;

    public Bridginess() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        //if (graphController != null && graphController.getModel() != null) {
        //    exclNeighbors = graphController.getModel().getExclNeighbors();
        //}
    }
    
    //UNDIRECTED GRAPHS ONLY, FOR NOW; SORRY
    @Override
    /*
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        HierarchicalGraph graph = null;
        
        //if (isDirected) {
        //    graph = graphModel.getHierarchicalDirectedGraphVisible();
        //} else {
            //graph = graphModel.getHierarchicalUndirectedGraphVisible();
        graph = graphModel.getHierarchicalUndirectedGraphVisible();
        //}
        execute(graph, attributeModel);
    }
    */

    public void execute(GraphModel graphModel, AttributeModel am) {
  //public void execute(HierarchicalGraph hgraph, AttributeModel am) {
        
        isCanceled = false;
        AttributeTable nodetable = am.getNodeTable();
        AttributeColumn bridginessCol = nodetable.getColumn(BRIDGINESS);
  
        if (bridginessCol == null) {
            bridginessCol = nodetable.addColumn(BRIDGINESS, "Bridginess", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        
        Graph graph = graphModel.getGraphVisible();

        graph.readLock();
        
        N = graph.getNodeCount();
        
        bridginess = new double[N];
        
        //shortestPaths = 0;
       
        Progress.start(progress, graph.getNodeCount());

        HashMap<Node, Integer> indicies = new HashMap<Node, Integer>();
        int index = 0;
        for (Node s : graph.getNodes()) {
            indicies.put(s, index);
            index++;
        }
        
        //REIMPLEMENTATION
        for (Node source : graph.getNodes()) {
            System.out.println("source " + source.getId());
            //LinkedList<Node>[] P = new LinkedList[N]; //Shortest PATHS
        
            AbstractShortestPathAlgorithm algorithm;
                //if (gc.getModel().getGraphVisible() instanceof DirectedGraph) {
                //    algorithm = new BellmanFordShortestPathAlgorithm((DirectedGraph) gc.getModel().getGraphVisible(), sourceNode);
                //} else {
            algorithm = new DijkstraShortestPathAlgorithm(graph, source);
            
                //}
            algorithm.compute();
            

            //int s_index = indicies.get(source);
            System.out.println("computed " + algorithm.getDistances().get(source.getNodeData()));
            
            for (Node target : graph.getNodes()) {
                if ( source != target ) {
                    System.out.println("target " + target.getId());
                    
                    //int t_index = indicies.get(target);
                    
                    double distance;
                    if ((distance = algorithm.getDistances().get(target.getNodeData())) != Double.POSITIVE_INFINITY) {
                    
                        if (distance  > 0) {
                            System.out.println("dist " + distance);
                        }
                    }
                }
                
            }
            
        }
        
        /****************
        //modified optimized algorithm - Brandes 2001 -----

        HashMap<Node, Integer> indicies = new HashMap<Node, Integer>();
        int index = 0;
        for (Node s : hgraph.getNodes()) {
            indicies.put(s, index);
            index++;
        }
        
        

            int count = 0;
            for (Node s : hgraph.getNodes()) {

                Stack<Node> S = new Stack<Node>();

                LinkedList<Node>[] P = new LinkedList[N];
                double[] theta = new double[N];
                int[] d = new int[N];
                for (int j = 0; j < N; j++) {
                    P[j] = new LinkedList<Node>();
                    theta[j] = 0;
                    d[j] = -1;
                }

                int s_index = indicies.get(s);

                theta[s_index] = 1;
                d[s_index] = 0;

                LinkedList<Node> Q = new LinkedList<Node>();
                Q.addLast(s);
                
                while (!Q.isEmpty()) {
                    Node v = Q.removeFirst();
                    S.push(v);
                    int v_index = indicies.get(v);
                    System.out.println("*** v " + v + " v_index " + v_index);

                    EdgeIterable edgeIter = null;
                    //if (isDirected) {
                    //    edgeIter = ((HierarchicalDirectedGraph) hgraph).getOutEdgesAndMetaOutEdges(v);
                    //} else {
                    edgeIter = hgraph.getEdgesAndMetaEdges(v);
                    //}

                    for (Edge edge : edgeIter) {
                        Node reachable = hgraph.getOpposite(v, edge);

                        int r_index = indicies.get(reachable);
                        if (d[r_index] < 0 ) {
                            Q.addLast(reachable);
                            d[r_index] = d[v_index] + 1;
                        }
                        if (d[r_index] == (d[v_index] + 1)) {
                            theta[r_index] = theta[r_index] + theta[v_index];
                            P[r_index].addLast(v);
                        }
                    }
                }
             
                
                double[] delta = new double[N];
                while (!S.empty()) {
                    Node w = S.pop();
                    int w_index = indicies.get(w);
                    //System.out.println("EX " + exclNeighbors + " Wi " + w_index + " PWi " + P[w_index] + " PWi.s " + P[w_index].size());
                    System.out.println("++  w " + w + " PW " + P[w_index] + " PWi.s " + P[w_index].size());
                    ListIterator<Node> iter1 = P[w_index].listIterator();
                    //---------filter out neighbors here--------------
                    if ( P[w_index].size() > exclNeighbors ) {

                        while (iter1.hasNext()) {
                            Node u = iter1.next();
                            int u_index = indicies.get(u);
                            delta[u_index] += (theta[u_index] / theta[w_index]) * (1 + delta[w_index]);
                            System.out.println(" u_index " + u_index + " w_index " + w_index);
                        }
                        if (w != s) {
                            bridginess[w_index] += delta[w_index];
                        }
                    } //else {
                      // System.out.println("Skipped w_index=" + w_index + " size " + P[w_index].size() );
                    //}
                
                }

               
                count++;
                if (isCanceled) {
                    hgraph.readUnlockAll();
                    return;
                }
                Progress.progress(progress, count);

            }
            
            for (Node s : hgraph.getNodes()) {
                AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
                int s_index = indicies.get(s);

                //if (!isDirected) {
                bridginess[s_index] /= 2;
                //}
                //if (isNormalized) {
                //    closeness[s_index] = (closeness[s_index] == 0) ? 0 : 1.0 / closeness[s_index];
                //    betweenness[s_index] /= isDirected ? (N - 1) * (N - 2) : (N - 1) * (N - 2) / 2;
                //}

                row.setValue(bridginessCol, bridginess[s_index]);
                
                avgBridginess += bridginess[s_index];
        }
            
        avgBridginess /= N; //WRONG!
 ****************/
        
            
        //hgraph.readUnlock();
        graph.readUnlock();                
            /*
            hgraph.readUnlockAll();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                graph.readUnlockAll();
            */
     }
       
       
   
    @Override
    public boolean cancel() {
        this.isCanceled = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        this.progress = pt;
    }

    @Override
    public String getReport() {
        String report = "Result goes here.";
        return report;
    }

    public void setExclNeighbors(int exclNeighbors) {
        this.exclNeighbors = exclNeighbors;
    }
    
    public int getExclNeighbors() {
        return exclNeighbors;
    }
    
    public double getAvgBridginess() {
        return avgBridginess;
    }
    
}
