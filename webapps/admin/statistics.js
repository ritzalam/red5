/**
 * Browser independent method to add events to elements.
 */
function addEvent(element, event, method) {
    if (element.addEventListener) element.addEventListener(event, method, false);
    else element.attachEvent("on"+event, method);
}

/**
 * Construct event handler that will be called on clicks in the menu.
 */
function updateScopeEvent(path, id) {
    return function(event) {
        updateScope(path, id);
        if (event.stopPropagation)
            event.stopPropagation();
        else
            event.cancelBubble = true;
    }
}

/**
 * Get informations for scope at given path and insert menu in element with given id.
 */
function updateScope(path, id) {
    var element = document.getElementById(id);
    if (!addSubScopes(path, element))
        return;
    
    updateScopeAttributes(path, document.getElementById("scope_contents"));
}

/**
 * Add entries to the navigation menu on the left depending on the
 * subscopes of the passed path.
 */
function addSubScopes(path, element) {
    try {
        var scopes = server.scopes.getScopes(path);
    } catch (err) {
        alert(err);
        return false;
    }
    
    submenu = document.createElement("ul");
    element.appendChild(submenu);
    for (var i=0; i<scopes.length; i++) {
        // Create nodes for each subscope
        var name = scopes[i];
        var id = path.replace("/", "_")+"_"+name;
        var node = document.getElementById(id);
        if (node) {
            // Delete existing child nodes
            var pos = 0;
            while (node.childNodes.length > pos) {
                var child = node.childNodes[pos];
                if (child.nodeType == 3) {
                    // Skip text nodes
                    pos += 1;
                    continue;
                }
                node.removeChild(child);
            }
        } else {
            // Create new subnode
            var node = document.createElement("li");
            node.appendChild(document.createTextNode(name));
            node.id = id;
            addEvent(node, "click", updateScopeEvent(path+"/"+name, node.id));
            submenu.appendChild(node);
        } 
    }
    
    return true;
}

/**
 * Create table containing the attributes of a scope.
 */
function updateScopeAttributes(path, container) {
    while (container.childNodes.length > 0)
        container.removeChild(container.firstChild);
    
    try {
        var attributes = server.scopes.getScopeAttributes(path);
    } catch (err) {
        alert(err);
        return false;
    }
    
    var table = null;
    var idx = 1;
    for (var name in attributes) {
        if (!table) {
            h3 = document.createElement("h3");
            h3.appendChild(document.createTextNode("Attributes"));
            container.appendChild(h3);
            
            table = document.createElement("table");
            container.appendChild(table);
            row = table.insertRow(0);

            th = document.createElement("th");
            th.appendChild(document.createTextNode("Name"));
            row.appendChild(th);
            
            th = document.createElement("th");
            th.appendChild(document.createTextNode("Value"));
            row.appendChild(th);
        }
        
        row = table.insertRow(idx++);
        td = row.insertCell(0);
        td.appendChild(document.createTextNode(name));
        
        td = row.insertCell(1);
        td.appendChild(document.createTextNode(attributes[name]));
    }
    
    if (container.childNodes.length == 0)
        container.appendChild(document.createTextNode("Scope has no attributes."));
    
    return true;
}