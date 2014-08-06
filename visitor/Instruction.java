package visitor;
import java.util.*;

public class Instruction{
	Integer id;
	String instruction;
	String type;
	HashSet<String> in;
	HashSet<String> out;
	HashSet<String> def;
	HashSet<String> use;
	ArrayList<Instruction> succ;
	String jumpLabel;

	public Instruction(Integer id, String instruction, String type){
		this.id = id;
		this.instruction = instruction;
		this.type = type;
		this.in = new HashSet<String>();
		this.out = new HashSet<String>();
		this.def = new HashSet<String>();
		this.use = new HashSet<String>();
		this.succ = new ArrayList<Instruction>();
		this.jumpLabel = jumpLabel;
	}

	public Integer getId(){
		return id;
	}

	public String getInstruction(){
		return instruction;
	}

	public String getType(){
		return type;
	}

	public HashSet<String> getIn(){
		return in;
	}

	public HashSet<String> getOut(){
		return out;
	}

	public HashSet<String> getDef(){
		return def;
	}

	public HashSet<String> getUse(){
		return use;
	}

	public void setJumpLabel(String jumpLabel){
		this.jumpLabel = jumpLabel;
	}

	public String getJumpLabel(){
		return jumpLabel;
	}

	public ArrayList<Instruction> getSucc(){
		return succ;
	}
}