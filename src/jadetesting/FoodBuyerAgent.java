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

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.LinkedList;

public class FoodBuyerAgent extends Agent {
    // The name of the food to buy

    private String targetFoodName;
    // The list of known seller agents
    private AID[] sellerAgents;

    private FoodBuyerGui myGui;

    Object[] args;
    boolean canRun;

    // Put agent initializations here
    protected void setup() {
        // Printout a welcome message
        System.out.println("");
        System.out.println("Halo! Agen-Buyer " + getAID().getName() + " telah siap.");
        System.out.println("Buyer is now active");

        canRun = false;
        myGui = new FoodBuyerGui(this);
        myGui.showGui();

        // Get the name of the food to buy as a start-up argument                
        startingAddBehaviour();
    }

    protected void startingAddBehaviour() {
        // Add a TickerBehaviour that schedules a request to seller agents every 10 seconds
        addBehaviour(new TickerBehaviour(this, 10000) { //10 dtk jdnya
            protected void onTick() {
                if (canRun) {
                    System.out.println("");
                    System.out.println("Target item is " + targetFoodName);
                    System.out.println("Buyer trying to buy " + targetFoodName);
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("food-selling");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Buyer searching the following seller agents:");
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    System.out.println("");
                    System.out.println("Asking item from sellers...");
                    myAgent.addBehaviour(new RequestPerformer());
                }
            }
        });
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        myGui.dispose();

        System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
        System.out.println("");
    }

    public void setArgs(String food) {
//            this.args[0]=food;
        targetFoodName = food;
        canRun = true;
        myGui.dispose();
    }

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
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetFoodName);
                    cfp.setConversationId("food-trade");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("food-trade"),
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
                        if (repliesCnt >= sellerAgents.length) {
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    System.out.println("Buyer try to buy item from best seller...");
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetFoodName);
                    order.setConversationId("food-trade");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("food-trade"),
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
                            System.out.println(targetFoodName + " successfully purchased from seller agent... " + reply.getSender().getName() + " to buyer agent.");
                            System.out.println("Price = " + bestPrice);
                            System.out.println("Buyer got the item, Item retrieved");
                            System.out.println("");
                            myAgent.doDelete();
                        } else {
                            System.out.println("Buyer attempt failed: requested food already sold.");
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
