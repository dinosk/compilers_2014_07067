/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import syntaxtree.*;
import visitor.*;

import java.io.*;
import java.util.*;
/**
 *
 * @author dinossimpson
 */
public class Main {
    
    

    public static void main(String[] args){
        HashMap<String, HashMap<String, HashSet<String>>> interferenceMap;
        HashMap<String, HashMap<String, ArrayList<String>>> procStats;
        HashMap<String, HashMap<String, String>> registerMap;
        HashMap<String, Instruction> labelInstructions;
        HashMap<String, Stack<String>> spillStack;
        HashMap<String, ArrayList<Instruction>> cfg;
        ArrayList<String> orderOfProcs;
        for(String filename : args){
            try{
                File file = new File(filename+".kg");
                if ( !file.exists() ) 
                  file.createNewFile();
                else {
                    boolean success = file.delete();
                    if (!success)
                        throw new IllegalArgumentException("Delete: deletion of older file failed");
                    file.createNewFile();
                }
                  
                FileWriter fstream = new FileWriter(filename+".kg");
                BufferedWriter out = new BufferedWriter(fstream);
                SpigletParser parser = new SpigletParser( new FileReader(filename) );

                Goal root = parser.Goal();
                StatsCounter sc = new StatsCounter(filename, filename);
                System.out.println("Got the stats!");
                root.accept( sc, filename);
                procStats = sc.getStats();
                orderOfProcs = sc.getOrder();

                LivenessVisitor liveVisitor = new LivenessVisitor(procStats);
                root.accept( liveVisitor, filename );
                System.out.println("liveVisitor finished!");
                liveVisitor.calcRegisterMap();
                System.out.println("Graph Coloring finished!");
                registerMap = liveVisitor.getRegisterMap();
                spillStack = liveVisitor.getSpillStack();
                cfg = liveVisitor.getCFG();
                interferenceMap = liveVisitor.getInterferenceMap();
                labelInstructions = liveVisitor.getLabelInstructions();

                root.accept( new KangaTranslator(procStats, registerMap, spillStack, cfg, interferenceMap, labelInstructions, orderOfProcs, out), filename );
            }
            catch(Exception e){
                System.out.println(e.toString());
            }
        }
    }
}
