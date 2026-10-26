package dev.riserecode;
import dev.riserecode.cosmetics.*;
import java.nio.file.*;
/** Verifies the complete prepared publication before any catalog upload. */
public final class CosmeticsCatalogTool {
    private CosmeticsCatalogTool(){ }
    public static void main(String[] args)throws Exception{
        Path root=Path.of("build/cosmetics-catalog/v1");int assets=0,models=0;
        for(var d:CosmeticsLibrary.parse(Files.readAllBytes(root.resolve("index.json")))){
            if(d.asset().equals("none")){continue;}byte[] bytes=Files.readAllBytes(root.resolve(d.asset()));
            if(bytes.length!=d.bytes()||!CosmeticsLibrary.hash(bytes).equals(d.sha256())){throw new AssertionError("Invalid published asset: "+d.id());}
            if(d.category()==CosmeticCategory.COMPANION){var model=CosmeticModel.read(bytes);model.sample(.3F,true);model.sample(.8F,false);models++;}else{CosmeticModel.png(bytes,2048);}assets++;
        }
        System.out.println("Prepared catalog verified: "+assets+" assets, "+models+" animated models.");
    }
}
