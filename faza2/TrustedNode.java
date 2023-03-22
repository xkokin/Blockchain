// Meno študenta:Hlib Kokin
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* TrustedNode označuje uzol, ktorý dodržuje pravidlá (nie je byzantský) */
public class TrustedNode implements Node {
    private double p_graph;
    private double p_byzantine;
    private double p_txDistribution;
    private int numRounds;
    private boolean[] followees;
    private Set<Transaction> pendingTransactions;

    public TrustedNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_byzantine = p_byzantine;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void followeesSet(boolean[] followees) {
        this.followees = followees;
    }

    public void pendingTransactionSet(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> followersSend() {
        return this.pendingTransactions;
    }


    public void followeesReceive(ArrayList<Integer[]> candidates) {
        for (Integer[] c : candidates) {
            Transaction cur = new Transaction(c[0]);
            if (followees[c[1]]) { //&& pendingTransactions.contains(cur)
                this.pendingTransactions.add(cur);
            }

        }
    }
}
