/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package jadetesting2;
package examples.bookTrading;

import jadetesting.*;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.awt.List;
import java.util.LinkedList;

/**
 *
 * @author ASUS
 */
public class RegistratorYellowPage{
    LinkedList<FoodSellerAgent> lbsa;
    FoodSellerAgent bsa;

    public RegistratorYellowPage() {
        bsa = new FoodSellerAgent();
        this.lbsa = new LinkedList<FoodSellerAgent>();
    }
    
    
    protected void setup(){
        
//        addBehaviour(new OfferRequestsServer());
//        addBehaviour(new PurchaseOrdersServer());
    }
    
    public void registerToYellowPage(FoodSellerAgent fsa ,DFAgentDescription dfd) throws FIPAException{
        this.lbsa.add(fsa);
        DFService.register(fsa, dfd);
    }
    
    public void deregisterToYellowPage(FoodSellerAgent fsa) throws FIPAException{
        this.lbsa.remove(fsa);
        DFService.deregister(fsa);
    }
    
//    public void addSeller(FoodSellerAgent input){
//        this.lfsa.add(input);
//    }
//    
//    public void removeSeller(){
//        
//    }
    
}
