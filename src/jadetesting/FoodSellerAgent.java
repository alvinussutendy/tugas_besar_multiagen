/** ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.  *
 * GNU Lesser General Public License
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.  *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 **************************************************************** */
package examples.bookTrading;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FoodSellerAgent extends Agent {
    // The catalogue of foods for sale (maps the name of a food to its price)
//	private Hashtable catalogue;
    // The GUI by means of which the user can add foods in the catalogue

    private FoodSellerGui myGui;

    private GudangSeller gs;
    private RegistratorYellowPage registrator;

    private String targetFoodName;
    // The list of known seller agents
    private AID[] supplierAgents;

    boolean stillAskingSupplier;
    private Semaphore semaphore;

    // Put agent initializations here
    protected void setup() {
        System.out.println("");
        System.out.println("Seller Agent is Starting...");

        // Create and show the GUI
        myGui = new FoodSellerGui(this);
        myGui.showGui();

        // Create gudang penampung untuk seller (pengganti catalogue/kelas pembungkus catalogue)
        this.gs = new GudangSeller();
        this.registrator = new RegistratorYellowPage();
        this.semaphore = new Semaphore(0);

        // Register the food-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("food-selling");
        sd.setName("JADE-food-trading");

        dfd.addServices(sd);

        try {
//			DFService.register(this, dfd);
            this.registrator.registerToYellowPage(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Add the behaviour serving queries from buyer agents
        addBehaviour(new OfferRequestsServer());

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
//			DFService.deregister(this);
            this.registrator.deregisterToYellowPage(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Close the GUI
        myGui.dispose();
        // Printout a dismissal message
        System.out.println("Seller-agent " + getAID().getName() + " terminating.");
        System.out.println("");
    }

    /**
     * This is invoked by the GUI when the user adds a new food for sale
     */
    public void updateCatalogue(final String name, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
//				catalogue.put(name, new Integer(price));
                gs.putCatalogue(name, price);
                System.out.println(name + " inserted into catalogue seller. Price = " + price);
                System.out.println("");
            }
        });
    }

    protected void askSupplier(String target) throws FIPAException {
        targetFoodName = target;
        System.out.println("");
        System.out.println("Seller trying to ask supplier for: " + targetFoodName);
        // Update the list of seller agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("food-supplier");
        template.addServices(sd);

        DFAgentDescription[] result = DFService.search(this, template);
        System.out.println("Seller found the following supplier agents:");
        supplierAgents = new AID[result.length];
        for (int i = 0; i < result.length; ++i) {
            supplierAgents[i] = result[i].getName();
            System.out.println(supplierAgents[i].getName());
        }

        // Perform the request
        System.out.println("Seller asking Supplier for Item...");
        this.addBehaviour(new RequestPerformer());
    }

    /**
     * Inner class OfferRequestsServer. This is the behaviour used by
     * Food-seller agents to serve incoming requests for offer from buyer
     * agents. If the requested food is in the local catalogue the seller agent
     * replies with a PROPOSE message specifying the price. Otherwise a REFUSE
     * message is sent back.
     */
    private class OfferRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String name = msg.getContent();
                ACLMessage reply = msg.createReply();

//				Integer price = (Integer) catalogue.get(name);
                Integer price = gs.getCatalogue(name);
                if (price == null) {//klo null minta ke supplier
                    try {
                        System.out.println("");
                        System.out.println("Item not available on seller, try asking supplier...");
                        askSupplier(name);
                    } catch (FIPAException ex) {
                        Logger.getLogger(FoodSellerAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    price = gs.getCatalogue(name);
                }

                if (price != null) {
                    // The requested food is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                } else {
                    // The requested food is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
                System.out.println("Noted: While thread 'asking supplier' is processing, the food may not available for now in seller.");
                System.out.println("");
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

    /**
     * Inner class PurchaseOrdersServer. This is the behaviour used by
     * Food-seller agents to serve incoming offer acceptances (i.e. purchase
     * orders) from buyer agents. The seller agent removes the purchased food
     * from its catalogue and replies with an INFORM message to notify the buyer
     * that the purchase has been sucesfully completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String name = msg.getContent();
                ACLMessage reply = msg.createReply();

//				Integer price = (Integer) catalogue.remove(name);
                Integer price = gs.removeCatalogue(name);
                // disini ga perlu nyari ke supplier lagi karena barangnya udah di request di atas (pas di offer)

                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(name + " sold to agent buyer: " + msg.getSender().getName());
                } else {
                    // The requested food has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

    /**
     * Inner class RequestPerformer. This is the behaviour used by Food-buyer
     * agents to request seller agents the target food.
     */
    private class RequestPerformer extends Behaviour {

        private AID bestSeller; // The agent who provides the best offer 
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all supplier
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < supplierAgents.length; ++i) {
                        cfp.addReceiver(supplierAgents[i]);
                    }
                    cfp.setContent(targetFoodName);
                    cfp.setConversationId("food-supplier");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("food-supplier"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer 
                            int price = Integer.parseInt(reply.getContent());
                            if (bestSeller == null || price < bestPrice) {
                                // This is the best offer at present
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= supplierAgents.length) {
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    System.out.println("Seller try to buy item from best supplier...");
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetFoodName);
                    order.setConversationId("food-supplier");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("food-supplier"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(targetFoodName + " successfully purchased from supplier agent... " + reply.getSender().getName() + " to seller agent.");
                            System.out.println("Price = " + bestPrice);
                            updateCatalogue(targetFoodName, bestPrice);
                            //myAgent.doDelete();
                        } else {
                            System.out.println("Attempt failed: requested food already sold.");
                            System.out.println("");
                        }

                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: " + targetFoodName + " not available for sale");
                System.out.println("");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer
}
