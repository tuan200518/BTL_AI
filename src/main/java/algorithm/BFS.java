package algorithm;

import model.Grid;
import model.Node;
import java.util.*;

public class BFS implements PathFinder {
    public String name() { return "BFS"; }
    public SearchResult search(Grid grid) {
        long t=System.nanoTime();
        Queue<Node> q=new ArrayDeque<>(); Set<Node> seen=new HashSet<>(); List<Node> visited=new ArrayList<>();
        Node start=grid.start(), goal=grid.goal(); q.add(start); seen.add(start); Node end=null;
        while(!q.isEmpty()) { Node cur=q.poll(); visited.add(cur); if(cur.equals(goal)){end=cur;break;} for(Node nb:grid.neighbors(cur)) if(seen.add(nb)){nb.parent(cur);q.add(nb);} }
        List<Node> resultPath = path(end, start);
        return new SearchResult(resultPath, visited, System.nanoTime()-t, cost(grid, resultPath));
    }
    static int cost(Grid grid, List<Node> path){
        if(path.isEmpty()) return 0;
        int total=0;
        for(int i=1;i<path.size();i++) total += grid.costAt(path.get(i).row(), path.get(i).col());
        return total;
    }

    static List<Node> path(Node end, Node start){ List<Node> p=new ArrayList<>(); for(Node n=end;n!=null;n=n.parent())p.add(n); if(end==null)return List.of(); Collections.reverse(p); return p; }
}
