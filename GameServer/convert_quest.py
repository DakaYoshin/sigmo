import os
import re
import sys

# Mappings for method signatures
METHOD_MAP = {
    '__init__': 'public {ClassName}(int questId, String name, String descr)',
    'onEvent': 'public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)',
    'onAdvEvent': 'public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)',
    'onTalk': 'public String onTalk(L2Npc npc, L2PcInstance player)',
    'onFirstTalk': 'public String onFirstTalk(L2Npc npc, L2PcInstance player)',
    'onAttack': 'public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)',
    'onKill': 'public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)',
    'onSpawn': 'public String onSpawn(L2Npc npc)',
    'onSkillSee': 'public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)',
    'onAggroRangeEnter': 'public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)',
}

def convert_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Extract Quest Info
    folder_name = os.path.basename(os.path.dirname(file_path))
    class_name = folder_name[0].upper() + folder_name[1:] # e.g., q110... -> Q110...
    
    java_lines = []
    
    # Imports
    java_lines.append(f"package quests.{folder_name};\n")
    java_lines.append("import com.gameserver.model.actor.L2Npc;")
    java_lines.append("import com.gameserver.model.actor.instance.L2PcInstance;")
    java_lines.append("import com.gameserver.model.quest.Quest;")
    java_lines.append("import com.gameserver.model.quest.QuestState;")
    java_lines.append("import com.gameserver.model.quest.State;")
    java_lines.append("import com.gameserver.model.actor.L2Character;")
    java_lines.append("import com.gameserver.model.L2Skill;")
    java_lines.append("import com.gameserver.model.L2Object;")
    java_lines.append("\n")
    
    java_lines.append(f"public class {class_name} extends Quest\n{{")
    
    # Process lines
    indent_stack = [0] # Track indentation levels
    in_method = False
    
    quest_id_line = None
    qn = f"{folder_name}"
    
    variables = []
    states = ['CREATED', 'STARTED', 'COMPLETED']
    
    body_lines = []
    
    quest_init_args = None
    
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        
        # Skip empty lines or python comments (naive)
        if not stripped or stripped.startswith("#"):
            i += 1
            continue
            
        current_indent = len(line) - len(line.lstrip())
        
        # Handling indentation closing
        while current_indent < indent_stack[-1]:
            indent_stack.pop()
            body_lines.append("    " * len(indent_stack) + "}\n")

        # Variables (simple assignments at top level)
        if current_indent == 0 and "=" in line and "class" not in line and "QUEST" not in line and "State" not in line:
            parts = stripped.split("=")
            var_name = parts[0].strip()
            var_val = parts[1].strip()
            if var_name == "qn":
                qn = var_val.replace('"', '').replace("'", "")
            else:
                # Naive inference of type
                var_type = "int"
                if '"' in var_val or "'" in var_val:
                    var_type = "String"
                variables.append(f"    private static final {var_type} {var_name} = {var_val};")
            i += 1
            continue
            
        # Class definition - skip python class def
        if stripped.startswith("class Quest"):
            i += 1
            continue
            
        # Method definition
        if stripped.startswith("def "):
            match = re.search(r"def\s+(\w+)\s*\(self,?(.*)\):", stripped)
            if match:
                method_name = match.group(1)
                args = match.group(2)
                
                if method_name in METHOD_MAP:
                    indent_stack.append(current_indent + 2) # Assume python indent is 2 or 4 spaces
                    # Using indentation assumptions is tricky, let's force a block open
                    # java indent will be 1 tab/4 spaces inside class
                    
                    sig = METHOD_MAP[method_name]
                    if "{ClassName}" in sig:
                        sig = sig.format(ClassName=class_name)
                        body_lines.append(f"\n    {sig}\n    {{")
                        # Constructor specific logic
                        body_lines.append(f"        super(questId, name, descr);")
                        body_lines.append(f"        addState(CREATED);")
                        body_lines.append(f"        addState(STARTED);")
                        body_lines.append(f"        addState(COMPLETED);")
                        body_lines.append(f"        setInitialState(CREATED);")
                    else:
                        body_lines.append(f"\n    @Override\n    {sig}\n    {{")
                        if "onAdvEvent" in method_name:
                             body_lines.append(f"        QuestState st = player.getQuestState(qn);")
                             body_lines.append(f"        if (st == null) return null;")
                        elif "onTalk" in method_name:
                             body_lines.append(f"        String htmltext = \"<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>\";")
                             body_lines.append(f"        QuestState st = player.getQuestState(qn);")
                             body_lines.append(f"        if (st == null) return htmltext;")
                             body_lines.append(f"        int npcId = npc.getNpcId();")
                             body_lines.append(f"        int cond = st.getInt(\"cond\");")
                             body_lines.append(f"        State id = st.getState();") # Map python 'state'/'id' to java
                    
                    i += 1
                    continue

        # Logic Conversion
        # This is where it gets messy. We will try some standard replacements
        
        # indent for java
        base_indent = "    " * (len(indent_stack))
        
        # Python to Java syntax
        java_line = stripped
        
        # if/elif/else
        if java_line.startswith("if "):
            java_line = java_line.replace("if ", "if (", 1).rstrip(":") + ") {"
            indent_stack.append(current_indent + 2) # Python usually indents 2 or 4 chars
        elif java_line.startswith("elif "):
            java_line = java_line.replace("elif ", "else if (", 1).rstrip(":") + ") {"
            indent_stack.append(current_indent + 2)
        elif java_line.startswith("else:"):
            java_line = "else {"
            indent_stack.append(current_indent + 2)
            
        # Basic API calls
        java_line = java_line.replace("self.questItemIds = ", "this.questItemIds = new int[] ") # list to array
        java_line = java_line.replace("[", "{").replace("]", "}") # list to array braces for init
        
        # JQuest specific
        if "JQuest.__init__" in java_line:
             i += 1
             continue # Handled in constructor
             
        # String concatenation
        # st.get("cond") -> st.getInt("cond") usually or st.get("cond") depends. In python they cast int(st.get("cond"))
        
        # Return
        if java_line.startswith("return "):
             java_line += ";"
             
        # Assignments
        if "=" in java_line and not java_line.endswith(";"):
             if not java_line.endswith("{"):
                 java_line += ";"

        # Python boolean
        java_line = java_line.replace(" and ", " && ").replace(" or ", " || ").replace(" not ", " ! ")
        
        # Fix st.getQuestItemsCount(ITEM) in boolean context -> st.getQuestItemsCount(ITEM) > 0
        # This is hard to regex generally without lookahead/lookbehind
        
        body_lines.append(base_indent + java_line + "\n")
        
        i += 1
    
    # Close any remaining blocks
    while len(indent_stack) > 1:
        indent_stack.pop()
        body_lines.append("    " * len(indent_stack) + "}\n")
        
    # Process footer (QUEST = ...)
    # This info is usually for the main() method or registration
    
    # States
    for state in states:
         java_lines.append(f"    private static final State {state} = new State(\"{state.capitalize()}\", null);")
    java_lines.append(f"    private static final String qn = \"{qn}\";")
    
    # Add variables
    for var in variables:
        java_lines.append(var)
        
    # Add body
    java_lines.extend(body_lines)
    
    # Add main
    java_lines.append(f"\n    public static void main(String[] args)\n    {{")
    java_lines.append(f"        new {class_name}(-1, qn, \"{folder_name}\");") # ID often needs lookup
    java_lines.append(f"    }}\n")
    
    java_lines.append("}")
    
    # Write result
    dest_path = os.path.join(os.path.dirname(file_path), class_name + ".java")
    with open(dest_path, 'w', encoding='utf-8') as f:
        f.writelines(java_lines)
        
    print(f"Generated {dest_path}")

convert_file(sys.argv[1])
