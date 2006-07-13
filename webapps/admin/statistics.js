var state = "";
var loading = false;

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
 * Update the "last modified" area of the page.
 */
function updateDate() {
    var last_updated = document.getElementById("last_updated_date");
    while (last_updated.childNodes.length > 0)
        last_updated.removeChild(last_updated.firstChild);
    last_updated.appendChild(document.createTextNode(new Date().toLocaleString()));
}

/**
 * Get informations for scope at given path and insert menu in element with given id.
 */
function updateScope(path, id, noUpdate) {
    var element = document.getElementById(id);
    if (!addSubScopes(path, element))
        return;
    
    var name = document.getElementById("scope_name");
    if (!name.firstChild)
        name.appendChild(document.createTextNode(""));
    name.firstChild.nodeValue = " for " + (path ? path : "root");
    updateScopeAttributes(path, document.getElementById("scope_contents"));
    updateSharedObjects(path, document.getElementById("scope_sharedobjects"));
    if (!noUpdate && encodeURIComponent(path) != state) {
        state = encodeURIComponent(path);
        window.location.hash = state;
    }
    updateDate();
}

/**
 * Add entries to the navigation menu on the left depending on the
 * subscopes of the passed path.
 */
function addSubScopes(path, element) {
    // Delete existing child nodes
    var pos = 0;
    while (element.childNodes.length > pos) {
        var child = element.childNodes[pos];
        if (child.nodeType == 3) {
            // Skip text nodes
            pos += 1;
            continue;
        }
        element.removeChild(child);
    }
    
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
        var id = path+"_"+name;
        while (id.indexOf("/") >= 0)
            id = id.replace("/", "_")
        var node = document.createElement("li");
        node.appendChild(document.createTextNode(name));
        node.id = id;
        addEvent(node, "click", updateScopeEvent(path+"/"+name, node.id));
        submenu.appendChild(node);
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
            th.className = "property";
            th.appendChild(document.createTextNode("Name"));
            row.appendChild(th);
            
            th = document.createElement("th");
            th.appendChild(document.createTextNode("Value"));
            row.appendChild(th);
        }
        
        row = table.insertRow(idx++);
        td = row.insertCell(0);
        td.className = "property";
        td.appendChild(document.createTextNode(name));
        
        td = row.insertCell(1);
        td.appendChild(document.createTextNode(attributes[name]));
    }
    
    if (container.childNodes.length == 0)
        container.appendChild(document.createTextNode("Scope has no attributes."));
    
    return true;
}

/**
 * Serializ a property that was received from Red5.
 */
function outputProperty(property, container) {
    if (isNaN(property) && property instanceof Object) {
        dl = document.createElement("dl");
        container.appendChild(dl);
        for (var name in property) {
            dt = document.createElement("dt");
            outputProperty(name, dt);
            dl.appendChild(dt);
            
            dd = document.createElement("dd");
            dl.appendChild(dd);
            outputProperty(property[name], dd);
        }
        return;
    }
    
    // Basic object
    container.appendChild(document.createTextNode(property));
}

/**
 * Create listing of shared objects in a scope.
 */
function updateSharedObjects(path, container) {
    while (container.childNodes.length > 0)
        container.removeChild(container.firstChild);
    
    try {
        var objects = server.scopes.getSharedObjects(path);
    } catch (err) {
        alert(err);
        return false;
    }
    
    for (var name in objects) {
        var table = null;
        var idx = 1;
        sub_container = document.createElement("div");
        sub_container.className = "sharedObject";
        container.appendChild(sub_container);
        h4 = document.createElement("h4");
        info = objects[name];
        if (info[0])
            h4.appendChild(document.createTextNode("Persistent shared object \"" + name + "\""));
        else
            h4.appendChild(document.createTextNode("Non-persistent shared object \"" + name + "\""));
        sub_container.appendChild(h4);
        for (var property in info[1]) {
            if (!table) {
                table = document.createElement("table");
                container.appendChild(table);
                row = table.insertRow(0);

                th = document.createElement("th");
                th.className = "property";
                th.appendChild(document.createTextNode("Property"));
                row.appendChild(th);
                
                th = document.createElement("th");
                th.appendChild(document.createTextNode("Value"));
                row.appendChild(th);
            }
            
            row = table.insertRow(idx++);
            td = row.insertCell(0);
            td.className = "property";
            td.appendChild(document.createTextNode(property));
            
            td = row.insertCell(1);
            outputProperty(info[1][property], td);
        }
    }
    
    if (container.childNodes.length == 0)
        container.appendChild(document.createTextNode("Scope has no shared objects."));
    
    return true;
}

/**
 * Checks for changes in the URL.
 */
function handleHistory() {
    current = window.location.hash;
    current = current.substring(1);
    if (current && (decodeURIComponent(current) != decodeURIComponent(state))) {
        initializeData();
    }
    return true;
}

/**
 * Called periodically to reload the statistics.
 */
function reloadStatistics() {
    if (loading)
        return;
    
    loading = true;
    try {
        initializeData();
    } finally {
        loading = false;
    }
    return true;
}

/**
 * Load statistics based on the URL.
 */
function initializeData() {
    state = window.location.hash;
    path = decodeURIComponent(state);
    if (path != "" && path[0] == "#")
        path = path.substring(1);
    
    if (path != "") {
        var parts = path.split("/");
        path = "";
        id = "menu_root";
        for (var i=0; i<parts.length; i++) {
            subpath = parts[i];
            if (subpath == "#")
                subpath = "";
            updateScope(path + subpath, id, true);
            path += subpath + "/";
            id = path + parts[i+1];
            while (id.indexOf("/") >= 0)
                id = id.replace("/", "_")
        }
        if (path)
            path = path.substring(0, path.length-1);
        state = encodeURIComponent(path);
    } else {
        state = "";
        updateScope('', 'menu_root');
    }
    
    if (state != window.location.hash)
        window.location.hash = state;

    // Check every 500ms for changes in the URL (back / forward button)
    window.setInterval("handleHistory()", 500);
    updateDate();
}

/**
 * Initialize statistics when page finished loading.
 */
addEvent(window, "load", initializeData);
