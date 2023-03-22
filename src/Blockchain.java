// Blockchain by mal na prevádzku funkcií udržiavať iba obmedzené množstvo uzlov
// Nemali by ste mať všetky bloky pridané do blockchainu v pamäti  
// pretože by to mohlo spôsobiť pretečenie pamäte.
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Blockchain {
    public static final int CUT_OFF_AGE = 2;

    private HashMap<ByteArrayWrapper, BlockNode> blocksByHash;
    private ArrayList<BlockNode> blocksByHeight;
    private TransactionPool tPool;
    private int maxHeight;

    // všetky potrebné informácie na spracovanie bloku v reťazi blokov
    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        // utxo pool na vytvorenie nového bloku na vrchu tohto bloku
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }
        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
         }
    }
    /**
     * vytvor prázdny blockchain iba s prvým (Genesis) blokom. Predpokladajme, že
     * {@code genesisBlock} je platný blok
     */
    public Blockchain(Block genesisBlock) {
        this.blocksByHash = new HashMap<>();
        this.blocksByHeight = new ArrayList<>();
        this.tPool = new TransactionPool();
        UTXOPool cnbase = new UTXOPool();
        cnbase.addUTXO(new UTXO(genesisBlock.getCoinbase().getHash(), 0),
                        genesisBlock.getCoinbase().getOutput(0));

        BlockNode genesis = new BlockNode(genesisBlock, null, cnbase);

        this.blocksByHash.put(new ByteArrayWrapper(genesisBlock.getHash()), genesis);
        this.blocksByHeight.add(0, genesis);
        this.maxHeight = 1;
    }

    // najdeme kam dat najmladsi node pevnej vysky,
    // najde a stane za posledny prvok na predchadzajucej vyske, takym padom vzdy vieme  ze
    // prvy prvok urcitej vysky je najmladsi
    private int findInsertionIndex(BlockNode block) {
        int index = 0;
        for (BlockNode b : blocksByHeight) {
            if (b.height >= block.height) {
                break;
            }
            index++;
        }
        return index;
    }




    /** Získaj najvyšší (maximum height) blok */
    public Block getBlockAtMaxHeight() {
        return blocksByHeight.get(blocksByHeight.size() - 1).b;
    }

    /** Získaj UTXOPool na ťaženie nového bloku na vrchu najvyššieho (max height) bloku */
    public UTXOPool getUTXOPoolAtMaxHeight() {
        return blocksByHeight.get(blocksByHeight.size() - 1).getUTXOPoolCopy();
    }

    /** Získaj pool transakcií na vyťaženie nového bloku */
    public TransactionPool getTransactionPool() {
        return this.tPool;
    }

    /**
     * Pridaj {@code block} do blockchainu, ak je platný. Kvôli platnosti by mali
     * byť všetky transakcie platné a blok by mal byť na
     * {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * Môžete napríklad vyskúšať vytvoriť nový blok nad blokom Genesis (výška bloku
     * 2), ak height blockchainu je {@code <=
     * CUT_OFF_AGE + 1}. Len čo {@code height > CUT_OFF_AGE + 1}, nemôžete vytvoriť
     * nový blok vo výške 2.
     *
     * @return true, ak je blok úspešne pridaný
     */
    public boolean blockAdd(Block block) {
        if (block.getPrevBlockHash() == null) return false;
        // zobereme hash rodica a najdeme rodica
        ByteArrayWrapper prevHash = new ByteArrayWrapper(block.getPrevBlockHash());
        BlockNode parent = blocksByHash.get(prevHash);
        // vytvorime handler, overime transakcie a zapiseme onoveny UTXOPool
        HandleTxs handlerTxs = new HandleTxs(parent.getUTXOPoolCopy());
        Transaction[] allTransactions = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] valids = handlerTxs.handler(allTransactions);
        if (allTransactions.length > valids.length) return false;
        // vytvorime novy blocknode
        BlockNode newBlock = new BlockNode(block, parent, handlerTxs.UTXOPoolGet());


        // border je minimalna vyska na ktorej moze byt rodic noveho bloku
        int border = this.maxHeight - CUT_OFF_AGE;
        if (parent.height + 1 <= border) return false;

        // pridame novy block do blockchainu
        ByteArrayWrapper newBlockHash = new ByteArrayWrapper(newBlock.b.getHash());
        blocksByHash.put(newBlockHash, newBlock);
        int index = findInsertionIndex(newBlock);
        blocksByHeight.add(index, newBlock);
        this.maxHeight++;
        // onovime transakcny pool
        for (Transaction tx : valids){
            transactionRemove(tx);
        }
        // skusime skratit starse bloky blockchainu ak je to mozne
        while (blocksByHeight.get(0).height <= this.maxHeight - CUT_OFF_AGE){
            //System.out.println("Heights " + )
            if (blocksByHeight.get(1).height != blocksByHeight.get(2).height) {

                blocksByHash.remove(new ByteArrayWrapper(blocksByHeight.get(0).b.getHash()));
                blocksByHeight.remove(0);
//                System.out.println("Block was removed from the beginning of the blockchain (no fork)");
            }
            else {
                int curHeight = blocksByHeight.get(0).height + 1;
                // lvlCnt je pocet Blokov ronakej vysky
                int lvlCnt = 0;
                // gap je pocet blokov o kolko jeden fork vyssi ako druhy najvyssi
                int gap = 0;
                BlockNode newStart = null;
                for(BlockNode b : blocksByHeight){
                    if (b.height == blocksByHeight.get(0).height) continue;
                    else{
                        // ak blok je rovnakej vysky ako minuly tak inkrementujeme pocet blokov na aktualnej vyske
                        if (b.height == curHeight) lvlCnt++;
                        // inac sme presli do dalsej vysky a musime skontrolovat kolko bolo blokov na minulej urovni
                        // ak blok nebol jeden - znamena to ze mame viac ako jednu vatvu, pokrocujeme
                        else if (lvlCnt != 1) {
                            curHeight++;
                            lvlCnt = 1;
                        }
                        // inac znamena to ze konecne teraz mame jednu vatvu a zvacsime gap a aktualnu vysku
                        else { //else if (lvlCnt == 1)
                            if(gap == 0) newStart = b;
                            gap++;
                            curHeight++;
                        }
                    }
                    // ak mame dostatocny gap medzi vatvami tak zostavime pre nas tu najvacsiu
                    if (gap >= CUT_OFF_AGE){
                        // zmazeme kazdy block co je nizsie ako ten na ktorom sme sa zastavili sa
                        for (int i = 0; i < blocksByHeight.indexOf(newStart); i++){
                            blocksByHash.remove(new ByteArrayWrapper(blocksByHeight.get(0).b.getHash()));
                            blocksByHeight.remove(0);
//                            System.out.println("Block was removed from the beginning of the blockchain (fork)");
                        }
                    }
                }
            }
        }

        return true;
    }

    /** Pridaj transakciu do transakčného poolu */
    public void transactionAdd(Transaction tx) {
        this.tPool.addTransaction(tx);
    }
    public void transactionRemove(Transaction tx) {
        this.tPool.removeTransaction(tx.getHash());
    }
}