package dev.riserecode.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/** Tolerant, cursor-aware completion, independent of Minecraft and command execution. */
public final class CommandCompletions {
    public record Completion(int start,int end,String text) { }
    private record Token(int start,String value) { }
    private CommandCompletions() { }
    public static List<Completion> suggest(CommandManager manager,String input,int cursor,BiFunction<Command,String[],List<String>> arguments){
        if(cursor<0||cursor>input.length()||!input.startsWith(manager.getPrefix())||cursor<manager.getPrefix().length()){return List.of();}
        int offset=manager.getPrefix().length();List<Token> tokens=tokens(input.substring(offset,cursor));Token current=tokens.getLast();
        List<String> candidates;
        if(tokens.size()==1){candidates=manager.getCommands().stream().flatMap(c->java.util.stream.Stream.concat(java.util.stream.Stream.of(c.getName()),c.getAliases().stream())).toList();}
        else{
            Command command=manager.getCommands().stream().filter(c->c.getName().equalsIgnoreCase(tokens.getFirst().value())||c.getAliases().stream().anyMatch(a->a.equalsIgnoreCase(tokens.getFirst().value()))).findFirst().orElse(null);
            if(command==null){return List.of();}candidates=arguments.apply(command,tokens.stream().skip(1).map(Token::value).toArray(String[]::new));
        }
        String fragment=current.value().toLowerCase(Locale.ROOT);int start=offset+current.start();int end=tokenEnd(input,start,cursor);
        return candidates.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(fragment)).distinct().sorted(String.CASE_INSENSITIVE_ORDER).map(s->new Completion(start,end,quote(s))).toList();
    }
    private static int tokenEnd(String input,int start,int cursor){
        char quote=0;boolean escaped=false;
        for(int i=start;i<input.length();i++){
            char c=input.charAt(i);
            if(escaped){escaped=false;}
            else if(c=='\\'){escaped=true;}
            else if(quote!=0){if(c==quote){quote=0;}}
            else if(c=='\''||c=='"'){quote=c;}
            else if(Character.isWhitespace(c)){return Math.max(cursor,i);}
        }
        return input.length();
    }
    private static String quote(String value){return value.chars().anyMatch(c->Character.isWhitespace(c)||c=='"'||c=='\\')?"\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"":value;}
    private static List<Token> tokens(String input){
        List<Token> result=new ArrayList<>();StringBuilder value=new StringBuilder();int start=0;char quote=0;boolean escape=false,active=false;
        for(int i=0;i<input.length();i++){
            char c=input.charAt(i);
            if(!active){start=i;active=true;}
            if(escape){value.append(c);escape=false;}
            else if(c=='\\'){escape=true;}
            else if(quote!=0){if(c==quote){quote=0;}else{value.append(c);}}
            else if(c=='\''||c=='"'){quote=c;}
            else if(Character.isWhitespace(c)){if(value.length()>0||start<i){result.add(new Token(start,value.toString()));}value.setLength(0);active=false;start=i+1;}
            else{value.append(c);}
        }
        result.add(new Token(active?start:input.length(),value.toString()));return result;
    }
}
