public class MainFaza3 {

    public static void main(String[] args){
        byte[] key_miner = new byte[32];
        byte[] key_bob = new byte[32];
        byte[] key_alice = new byte[32];

        for (int i = 0; i < 32; i++) {
            if (i<16) key_miner[i] = (byte) 1;
            else key_miner[i] = (byte) 0;
            key_bob[i] = (byte) 1;
            key_alice [i] = (byte) 0;
        }

        PRGen minerGen = new PRGen(key_miner);
        PRGen prGen_bob = new PRGen(key_bob);
        PRGen prGen_alice = new PRGen(key_alice);

        RSAKeyPair pk_miner = new RSAKeyPair(minerGen, 265);
        RSAKeyPair pk_bob = new RSAKeyPair(prGen_bob, 265);
        RSAKeyPair pk_alice = new RSAKeyPair(prGen_alice, 265);

        Block genesis = new Block(null, pk_miner.getPublicKey());
        genesis.blockFinalize();

        Blockchain blockchain = new Blockchain(genesis);

        HandleBlocks handler = new HandleBlocks(blockchain);

        handler.txProcess(new Transaction(6.25, pk_bob.getPublicKey()));
        Block first = handler.blockCreate(pk_bob.getPublicKey());
        if (first != null) System.out.println("First Block successfully added");
        else System.out.println("First Block rejected");


        Transaction twoOut = new Transaction(3.25, pk_alice.getPublicKey());
        twoOut.addOutput(3, pk_bob.getPublicKey());
        handler.txProcess(twoOut);
        Block second = handler.blockCreate(pk_alice.getPublicKey());
        if (second != null) System.out.println("Second Block successfully added");
        else System.out.println("Second Block rejected");



    }
}
