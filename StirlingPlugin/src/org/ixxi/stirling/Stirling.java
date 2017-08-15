package org.ixxi.stirling;

import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Math.log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.statistics.plugin.ChartUtils;

import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.TempDirUtils;
import org.gephi.utils.TempDirUtils.TempDir;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.util.Exceptions;


/**
 *
 * @author PJ@IXXI, MJ@MedialabSciencesPO, MM@IXXI
 */


public class Stirling implements Statistics, LongTask {
        
    public static final String NODEENTROPY = "nodeentropy";
    public static final String STIRLING = "nodestirling";
    public static final String STIRLINGMOD = "nodestirlingmod";
    
    //from "gephi-0.8.2/modules/StatisticsPlugin/src/main/java/org/gephi/statistics/plugin/Modularity.java"
    public static final String MODULARITY_CLASS = "modularity_class";
    //* */
    
    private double[] nodeEntropy; 
    private double[] nodeStirling;
    private double[] nodeStirlingMod;
    
    private double avgBridginess;
    
    
    private int N;
    
    private boolean isDirected;
    
    private ProgressTicket progress;
    
    private boolean isCanceled;
    private int shortestPaths;
    //private boolean isNormalized; //let's forget normalization for now
    
  
    private int exclNeighbors; //neighbors bridginess
    private int minPathLength; //path bridginess
    

    public Stirling() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            //exclNeighbors = graphController.getModel().getExclNeighbors();
            isDirected = graphController.getModel().isDirected();
        }
    }
    
    /**
     *
     * @param graphModel
     */
    
    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        HierarchicalGraph graph = null;        
        if (isDirected) {
            graph = graphModel.getHierarchicalDirectedGraphVisible();
        } else {
            graph = graphModel.getHierarchicalUndirectedGraphVisible();
        }
        execute(graph, attributeModel);
    }
    
  public void execute(HierarchicalGraph hgraph, AttributeModel am) {
        
        isCanceled = false;
        AttributeTable nodetable = am.getNodeTable();
        AttributeColumn nodeentropyCol = nodetable.getColumn(NODEENTROPY);
        AttributeColumn stirlingCol = nodetable.getColumn(STIRLING);
        
        AttributeColumn stirlingModCol = nodetable.getColumn(STIRLINGMOD);
        
        if (nodeentropyCol == null) {
            nodeentropyCol = nodetable.addColumn(NODEENTROPY, "Entropy", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (stirlingCol == null) {
            stirlingCol = nodetable.addColumn(STIRLING, "Stirling", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (stirlingModCol == null) {
            stirlingModCol = nodetable.addColumn(STIRLINGMOD, "StirlingMod", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
 

        hgraph.readLock();
        
        N = hgraph.getNodeCount();
                
        nodeEntropy = new double[N];
        nodeStirling = new double[N];
        nodeStirlingMod = new double[N];

        HashMap<Node, Integer> indicies = new HashMap<Node, Integer>();
        int index = 0;
        for (Node s : hgraph.getNodes()) {
            indicies.put(s, index);
            index++;
        }
        
        Progress.start(progress, hgraph.getNodeCount());
        int count = 0;
        
        //entropy
        //count communities pop.
        HashMap<Integer, Integer> communityPop = new HashMap<Integer, Integer>();
        HashMap<Integer, LinkedList<Node>> communityNodes = new HashMap<Integer, LinkedList<Node>>();
                      
        for (Node s : hgraph.getNodes()) {                       
            
            Integer community = (Integer) s.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
            if (communityPop.containsKey(community)) {
                Integer freq = communityPop.get(community);
                freq += 1;
                communityPop.put(community, freq);
                //insert member node in existing community                
                communityNodes.get(community).addLast(s);
            } else {
                communityPop.put(community, 1);
                //insert member node in not yet existing list
                LinkedList<Node> ll = new LinkedList<Node>();
                communityNodes.put(community, ll);
                communityNodes.get(community).addLast(s);
            }
        }
        
        Integer communityNum = communityPop.size();
        //System.out.println(communityNum + " communities found.");
        
        Double communityDistance[][] = new Double[communityNum+1][communityNum+1];
        Double communityDistanceInverse[][] = new Double[communityNum+1][communityNum+1];
        
        //initialize distances;
        //WARNING: presumes communities #0, #1, #2...#N (no gaps)
        for (int sComm = 0; sComm <= communityNum; sComm++) {
            for (int tComm = 0; tComm <= communityNum; tComm++) {
                communityDistance[sComm][tComm] = Double.POSITIVE_INFINITY;   
                //communityDistance[sComm][tComm] = 1000.;   
                communityDistanceInverse[sComm][tComm] = 0.0;   
                //communityDistanceInverse[sComm][tComm] = 0.0001;   
            }
        }
  
        
        for (Node s : hgraph.getNodes()) {
            Integer sComm = (Integer) s.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
            for (Node t : hgraph.getNeighbors(s)) {
                Integer tComm = (Integer) t.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
                //same community, not interesting

                if (sComm.intValue() == tComm.intValue()) {
                    continue;
                }
                
                double wl_st = (double) hgraph.getEdge(s, t).getWeight();
                System.out.println("communityNum = " + communityNum);
                System.out.println("sc " + sComm + " tc " + tComm + " wl_st " + wl_st);
                
                communityDistanceInverse[sComm][tComm] += wl_st;
            }
        }

        
        
        //find max distance (min inv distance)
        Double minInvDistance = Double.POSITIVE_INFINITY;
        for (int sComm = 0; sComm <= communityNum; sComm++) {
            for (int tComm = 0; tComm <= communityNum; tComm++) {
                if (communityDistanceInverse[sComm][tComm] != 0 && communityDistanceInverse[sComm][tComm] < minInvDistance) {
                minInvDistance = communityDistanceInverse[sComm][tComm];
                }
            }
        }
            
        Double maxDistance =  1/minInvDistance;
        //change infinities to 10*max stirling
        for (int sComm = 0; sComm <= communityNum; sComm++) {
            for (int tComm = 0; tComm <= communityNum; tComm++) {
            if (communityDistanceInverse[sComm][tComm] == 0 ) {
                communityDistanceInverse[sComm][tComm] = minInvDistance/10.0;
                } 
            }
        }
        
        //dump community distances
        for (int sComm = 0; sComm <= communityNum; sComm++) {
            for (int tComm = 0; tComm <= communityNum; tComm++) {
                communityDistance[sComm][tComm] = 1/communityDistanceInverse[sComm][tComm];
                System.out.println("commdist: s " + sComm + " t " + tComm + " dist: " + communityDistance[sComm][tComm]);
            }
        }

        
        
        
        for (Node s : hgraph.getNodes()) { 
            
           int s_index = indicies.get(s);
           Integer sCommunity = (Integer) s.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
        
           //ignore nodes from small communities
           //if (communityPop.get(sCommunity) < 100) {
           //    continue;
           //}
                      
           HashMap<Integer, Double> weightsByCommunity = new HashMap<Integer, Double>();           
           Double nodeLocalWeights = 0d;
           Double nodeTotalWeights = 0d;

           Double nodeWeightedDegree =0d; //stirling ponderation (squared)
           
           for (Node t : hgraph.getNeighbors(s)) {
                if (s == t) {
                    continue;
                }
                Edge e = (Edge) hgraph.getEdge(s, t);
                double w =  e.getWeight();               
                nodeWeightedDegree += w; 
           }

           for (Node t : hgraph.getNeighbors(s)) {
                if (s == t) {
                    continue;
                }
               
                Integer tCommunity = (Integer) t.getNodeData().getAttributes().getValue(MODULARITY_CLASS);                
                Double weight = (double) hgraph.getEdge(s, t).getWeight();

                //overall weights (for normalization)
                nodeTotalWeights += weight;
                
                //compute cumulated weights per community
//                if ("833".equals((String) s.getNodeData().getId())) {
//                   System.out.println("adding weight " + weight + " to node " + t.getNodeData().getId() + " to comm " + tCommunity); 
//                }

                
                if (sCommunity.intValue() == tCommunity.intValue()) {
                    nodeLocalWeights += weight;
                } else 
                    if (weightsByCommunity.containsKey(tCommunity)) {
                        Double oldWBC = weightsByCommunity.get(tCommunity);
                        weightsByCommunity.put(tCommunity, oldWBC + weight);
                    } else {
                        weightsByCommunity.put(tCommunity, weight);
                    }
                }

           
                     
           nodeEntropy[s_index] = 0.;
           for (Map.Entry<Integer, Double> entry : weightsByCommunity.entrySet()) {
                              
               Integer comm = entry.getKey();
               //System.out.println("comm " + comm + " size " + communityPop.get(comm));
               if (communityPop.get(comm) < 0) {
                   continue;
               }

               Double commWeights = entry.getValue();
              
               //System.out.println("commweights " + commWeights + " nodelocalweights " + nodeLocalWeights);
               
               Double p_iJ = commWeights/(commWeights+nodeLocalWeights);
               
               //System.out.println("p_iJ " + p_iJ);
               
               nodeEntropy[s_index] += (p_iJ) * (Double) log(p_iJ);
            }

          
           nodeStirling[s_index] = 0.;

           //add local community!
           //sNode community
           
           weightsByCommunity.put(sCommunity, nodeLocalWeights);
           

//           if ("833".equals((String) s.getNodeData().getId())) {
//                System.out.println("*** communities: " + weightsByCommunity.keySet() );
//                System.out.println("*** comm values: " + weightsByCommunity.values() );
//           }
           
           Iterator comm1iter = weightsByCommunity.entrySet().iterator();
           while (comm1iter.hasNext()) {
           
               Map.Entry comm1entry = (Map.Entry) comm1iter.next();
               Integer comm1 = (Integer) comm1entry.getKey();

//               if ("833".equals((String) s.getNodeData().getId())) {
//                   System.out.println(" -- ext loop on comm " + comm1); //entry.getValue());
//               }

               
               Double p_iJ1= (Double) comm1entry.getValue();

               Iterator comm2iter = weightsByCommunity.entrySet().iterator();
               while (comm2iter.hasNext()) {
                   
                   Map.Entry comm2entry = (Map.Entry) comm2iter.next();
                   Integer comm2 = (Integer) comm2entry.getKey();

//                        if ("833".equals((String) s.getNodeData().getId())) {
//                              System.out.println(" -- int loop on comm " + comm2); //entry.getValue());
//                        }


                        if (comm1.intValue() == comm2.intValue()) {
//                           if ("833".equals((String) s.getNodeData().getId())) {
//                              //
//                              System.out.println(" -- comm1=comm2 " + comm1+ " = " + comm2); 
//                            }

                            continue;
                        }

                        Double p_iJ2 = (Double) comm2entry.getValue();
                        Double d_j1j2 = (Double) communityDistance[comm1.intValue()][comm2.intValue()];
                        

                        //System.out.println("node: "+s.getNodeData().getId());
//                        if ("833".equals((String) s.getNodeData().getId())) {
//                            System.out.println("833: " + " p_iJ1 "+ p_iJ1 + " p_iJ2 "+ p_iJ2 + " d_j1j2 "+ d_j1j2);
//                            System.out.println("     " + " comm 1 " + comm1.intValue() + " comm2 " + comm2.intValue());
//                            System.out.println("     " + " wd " + nodeWeightedDegree + " p1.p2.d " + (p_iJ1 * p_iJ2 * d_j1j2));
//                        }

                        nodeStirling[s_index] += (p_iJ1 * p_iJ2 * d_j1j2) / 
                                (nodeWeightedDegree * nodeWeightedDegree);

                        //QUICK AND DIRTY attempt to modify Stirling, pm=1 if p>0, else pm=0
                        Double pm_iJ1 = 0.0d;
                        if (p_iJ1 > 0) {
                            pm_iJ1 = 1.0d;
                                    }
                        Double pm_iJ2 = 0.0d;
                        if (p_iJ2 > 0) {
                            pm_iJ2 = 1.0d;
                                    }
                        
                        //Why / (nodeWeightedDegree ^ 2) ???
                        nodeStirlingMod[s_index] += (pm_iJ1 * pm_iJ2 * d_j1j2) ;/// 
                                //(nodeWeightedDegree * nodeWeightedDegree);
                   }
               }    

            count++;
            if (isCanceled) {
                hgraph.readUnlockAll();
                return;
            }
            Progress.progress(progress, count);
        }

        //values to table
        for (Node s : hgraph.getNodes()) { 
            int s_index = indicies.get(s);
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();            
            row.setValue(nodeentropyCol, -nodeEntropy[s_index]); //minus sign away
            row.setValue(stirlingCol, nodeStirling[s_index]);  
            row.setValue(stirlingModCol, nodeStirlingMod[s_index]);  
        }
            
        hgraph.readUnlock();                
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

    private String createImageFile(TempDir tempDir, double[] pVals, String pName, String pX, String pY) {
     //distribution of values
     Map<Double, Integer> dist = new HashMap<Double, Integer>();
     for (int i = 0; i < N; i++) {
         Double d = pVals[i];
         if (dist.containsKey(d)) {
             Integer v = dist.get(d);
             dist.put(d, v + 1);
         } else {
             dist.put(d, 1);
         }
     }

     //Distribution series
     XYSeries dSeries = ChartUtils.createXYSeries(dist, pName);

     XYSeriesCollection dataset = new XYSeriesCollection();
     dataset.addSeries(dSeries);

     JFreeChart chart = ChartFactory.createXYLineChart(
             pName,
             pX,
             pY,
             dataset,
             PlotOrientation.VERTICAL,
             true,
             false,
             false);
     chart.removeLegend();
     ChartUtils.decorateChart(chart);
     ChartUtils.scaleChart(chart, dSeries, FALSE);//isNormalized);
     return ChartUtils.renderChart(chart, pName + ".png");
    }

    
    @Override
    public String getReport() {
        String htmlIMG1 = "";
        try {
            TempDir tempDir = TempDirUtils.createTempDir();
            htmlIMG1 = createImageFile(tempDir, nodeStirling, "Stirling indicator Distribution", "Value", "Count");
          } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        String report = "<HTML> <BODY> <h1>Graph Distance  Report </h1> "
                + "<hr>"
                + "<br>"
                + "Network Interpretation:  " + (isDirected ? "directed" : "undirected") + "<br />"
                + "<br /> <h2> Results: </h2>"
                + htmlIMG1 + "<br /><br />"
                + "<br /><br />" + "<h2> Algorithm: </h2>"
                + "Andrew Stirling, <i>On the Economics and Analysis of Diversity</i>, in SPRU Electronic Working Papers Series, #28, (1998)<br />"
                + "By Pablo Jensen (IXXI), Matteo Morini (IXXI)"
                + "</BODY> </HTML>";

        return report;
    }

    public void setMinPathLength(int minPathLength) {
        this.minPathLength = minPathLength;
    }
    
    public int getMinPathLength() {
        return minPathLength;
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
