import java.util.ArrayList;
import java.util.HashMap;

// Meno študenta: Hlib Kokin
// ID: 117991
public class HandleTxs {
    private UTXOPool actUTXOPool = null;
    /**
     * Vytvorí verejný ledger (účtovnú knihu), ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť bezpečnú kópiu
     * utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */
    public HandleTxs(UTXOPool utxoPool) {
        this.actUTXOPool = new UTXOPool(utxoPool);
    }

    /**
     * @return aktuálny UTXO pool. 
     * Ak nenájde žiadny aktuálny UTXO pool, tak vráti prázdny (nie nulový) objekt {@code UTXOPool}.
     */
    public UTXOPool UTXOPoolGet() {
        if (this.actUTXOPool != null)
            return this.actUTXOPool;
        else
            return this.actUTXOPool = new UTXOPool();
    }

    /**
     * @return true, ak 
     * (1) sú všetky nárokované výstupy {@code tx} v aktuálnom UTXO pool, 
     * (2) podpisy na každom vstupe {@code tx} sú platné, 
     * (3) žiadne UTXO nie je nárokované viackrát, 
     * (4) všetky výstupné hodnoty {@code tx}s sú nezáporné a 
     * (5) súčet vstupných hodnôt {@code tx}s je väčší alebo rovný súčtu jej
     *     výstupných hodnôt; a false inak.
     */
    public boolean txIsValid(Transaction tx) {
        ArrayList<UTXO> keys = new ArrayList<>(actUTXOPool.getAllUTXO());
        ArrayList<Transaction.Input> inputs = new ArrayList<>(tx.getInputs());
         if (this.actUTXOPool == null) return false;

        // (1)
        int cnt = 0;
        //ArrayList<UTXO> outInputs = new ArrayList<>();
        HashMap<Integer, UTXO> outInputs = new HashMap<>();
        for(Transaction.Input i: inputs){
            outInputs.put(cnt, new UTXO(i.prevTxHash, i.outputIndex));
            cnt++;
        }
        for (UTXO utxo : outInputs.values()){
            if (keys.contains(utxo)) cnt--;
        }

        if (cnt != 0) return false;
        System.out.println("Every Input is UTXO");
        // (2)
        cnt = 0;
        for (Transaction.Input i : inputs){
            UTXO curUtxo = outInputs.get(cnt);
            Transaction.Output curOutput = actUTXOPool.getTxOutput(curUtxo);
            if (!curOutput.address.verifySignature(tx.getDataToSign(cnt), i.signature)) return false;

            cnt++;
        }
        System.out.println("Every sign is valid");
        // (3)

        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                if (keys.get(i).equals(keys.get(j)))
                    if (keys.get(i).hashCode() == keys.get(j).hashCode()) return false;
            }
        }
        System.out.println("There are no double requested UTXOs");
        // (4)
        int sumO = 0;
        for (Transaction.Output o : tx.getOutputs()){
            sumO += o.value;
            if (o.value < 0) return false;
        }
        System.out.println("SumO is not equal 0");
        // (5)
        int sumi = 0;
        for (Transaction.Input i : inputs) {
            UTXO cur = new UTXO(i.prevTxHash, i.outputIndex);
            for (UTXO u : keys) {
                if (u.equals(cur)) sumi += actUTXOPool.getTxOutput(u).value;
            }
        }
        System.out.println("Sumi ="+ sumi + "; Sumo =" + sumO);

        if (sumi < sumO) return false;

        return true;
    }

    /**
     * Spracováva každú epochu (iteráciu) prijímaním neusporiadaného radu navrhovaných
     * transakcií, kontroluje správnosť každej transakcie, vracia pole vzájomne 
     * platných prijatých transakcií a aktualizuje aktuálny UTXO pool podľa potreby.
     */
    public Transaction[] handler(Transaction[] possibleTxs) {
        ArrayList<Transaction> valids = new ArrayList<>();
        for (Transaction t : possibleTxs){
            if (txIsValid(t)) valids.add(t);
        }
        int length = valids.size();
        Transaction[] res = new Transaction[length];
        int cnt = 0;
        for (Transaction t : valids){
            res[cnt] = t;
            cnt++;
        }
        return res;
    }
}
