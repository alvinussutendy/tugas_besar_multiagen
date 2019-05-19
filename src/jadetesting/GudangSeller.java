/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package jadetesting;
package examples.bookTrading;


import java.util.Hashtable;

/**
 *
 * @author ASUS
 */
public class GudangSeller {
    // The catalogue of foods for sale (maps the name of a food to its price)
	private Hashtable catalogue;
        
    GudangSeller(){
        this.catalogue = new Hashtable();
    }
    
    public void putCatalogue(String name, int price){
        this.catalogue.put(name, new Integer(price));
    }
    
    public Integer getCatalogue(String target){
        return (Integer)this.catalogue.get(target);
    }
    
    public Integer removeCatalogue(String target){
        return (Integer)this.catalogue.remove(target);
    }
}
