package algorithm;

import model.Grid;
import model.Node;
import java.util.*;

public class AStar implements PathFinder {
    public String name(){return "A*";}
    public SearchResult search(Grid grid){
        long t=System.nanoTime(); Node s=grid.start(), g=grid.goal();
        Map<Node,Integer> best=new HashMap<>(); Set<Node> closed=new HashSet<>(); List<Node> visited=new ArrayList<>();
        PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingInt(Node::fCost).thenComparingInt(Node::hCost));
        s.gCost(0); s.hCost(h(s,g)); best.put(s,0); open.add(s); Node end=null;
        while(!open.isEmpty()){Node cur=open.poll(); if(!closed.add(cur))continue; visited.add(cur); if(cur.equals(g)){end=cur;break;}
            for(Node nb:grid.neighbors(cur)){if(closed.contains(nb))continue; int ng=cur.gCost()+grid.costAt(nb.row(),nb.col()); if(ng<best.getOrDefault(nb,Integer.MAX_VALUE)){nb.gCost(ng);nb.hCost(h(nb,g));nb.parent(cur);best.put(nb,ng);open.add(nb);}}
        }
        List<Node> resultPath = BFS.path(end, s);
        return new SearchResult(resultPath, visited, System.nanoTime()-t, BFS.cost(grid, resultPath));
    }
    private int h(Node a,Node b){return Math.abs(a.row()-b.row())+Math.abs(a.col()-b.col());}
}
