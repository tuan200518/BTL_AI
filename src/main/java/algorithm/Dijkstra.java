package algorithm;

import model.Grid;
import model.Node;
import java.util.*;

public class Dijkstra implements PathFinder {
    public String name(){return "Dijkstra";}
    public SearchResult search(Grid grid){
        long t=System.nanoTime();
        Map<Node,Integer> dist=new HashMap<>(); Set<Node> done=new HashSet<>(); List<Node> visited=new ArrayList<>();
        PriorityQueue<Node> pq=new PriorityQueue<>(Comparator.comparingInt(Node::gCost));
        Node s=grid.start(), g=grid.goal(); s.gCost(0); dist.put(s,0); pq.add(s); Node end=null;
        while(!pq.isEmpty()){Node cur=pq.poll(); if(!done.add(cur))continue; visited.add(cur); if(cur.equals(g)){end=cur;break;} for(Node nb:grid.neighbors(cur)){int nd=cur.gCost()+grid.costAt(nb.row(),nb.col()); if(nd<dist.getOrDefault(nb,Integer.MAX_VALUE)){nb.gCost(nd);nb.parent(cur);dist.put(nb,nd);pq.add(nb);}}}
        List<Node> resultPath = BFS.path(end, s);
        return new SearchResult(resultPath, visited, System.nanoTime()-t, BFS.cost(grid, resultPath));
    }
}
