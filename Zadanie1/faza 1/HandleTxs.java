// Meno študenta: Hlib Kokin
// ID: 117991
public class HandleTxs {
    private actUTXOPool = null;
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
            return this.actUTXOPull;
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
        if (this.actUTXOPool == null) return false;

        // (1)
        int cnt = 0;
        for (UTXO utxo : this.actUTXOPool){
            cnt++;
            if (!tx.getOutputs().contains(getTxOutput(utxo))) return false;
        }
        if (tx.numOutputs() > cnt) return false;
        // (2)
        cnt = 0;

        for (Transaction.Input i : tx.getInputs()){
            byte[] message = tx.getDataToSign(cnt);
            if (!rsa.RSAKey.veifySignature(message, i.signature)) return false;
            cnt++;
        }
        // (3)
        List<UTXO>keys = new ArrayList<>(actUTXOPool.keySet());
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                if (keys.get(i).equals(keys.get(j)))
                    if (keys.get(i).hashCode() == keys.get(j).hashCode()) return false;
            }
        }
        // (4)
        int sumO = 0;
        for (Transaction.Output o : tx.getOutputs()){
            sumO += o.value;
            if (o.value < 0) return false;
        }
        // (5)
        int sumi = 0;
        for (Transaction.Input i : tx.getInputs()) {
            UTXO cur = new UTXO(i.prevTxHash, i.outputIndex);
            for (UTXO u : actUTXOPool) {
                if (u.equals(cur)) sumi += actUTXOPool.get(u).value;
            }
        }

        if (sumi < sumO) return false;

        return true;
    }

    /**
     * Spracováva každú epochu (iteráciu) prijímaním neusporiadaného radu navrhovaných
     * transakcií, kontroluje správnosť každej transakcie, vracia pole vzájomne 
     * platných prijatých transakcií a aktualizuje aktuálny UTXO pool podľa potreby.
     */
    public Transaction[] handler(Transaction[] possibleTxs) {
        Transaction res[];
        for (Transaction t : possibleTxs){
            if (txIsValid(t)) res.add(t);
        }
        return res;
    }
}
