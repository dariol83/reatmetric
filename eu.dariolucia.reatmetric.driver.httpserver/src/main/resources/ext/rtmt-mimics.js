const DATA_RTMT_BINDING_ID = "data-rtmt-binding-id";
const FILL_PREFIX = "data-rtmt-fill-color";
const STROKE_PREFIX = "data-rtmt-stroke-color";
const STROKE_WIDTH_PREFIX = "data-rtmt-stroke-width";
const VISIBILITY_PREFIX = "data-rtmt-visibility";
const TEXT_PREFIX = "data-rtmt-text";
const TRANSFORM_PREFIX = "data-rtmt-transform";
const BLINK_PREFIX = "data-rtmt-blink";
const ROTATE_PREFIX = "data-rtmt-rotate";
const WIDTH_PREFIX = "data-rtmt-width";
const HEIGHT_PREFIX = "data-rtmt-height";

const SUPPORTED_SVG_ACTIONS = [
	FILL_PREFIX,
	STROKE_PREFIX,
	STROKE_WIDTH_PREFIX,
	VISIBILITY_PREFIX,
	TEXT_PREFIX,
	TRANSFORM_PREFIX,
	BLINK_PREFIX,
	ROTATE_PREFIX,
	WIDTH_PREFIX,
	HEIGHT_PREFIX
];

const NULL_VALUE = "##NULL##";
const NO_BLINK = "none";
const NO_ROTATE = "none";

const operatorMap = [];
operatorMap["EQ"] = function (a,b) { return a == b; }
operatorMap["NQ"] = function (a,b) { return a != b; }
operatorMap["LTE"] = function (a,b) { return a == b || a < b; }
operatorMap["GTE"] = function (a,b) { return a == b || a > b; }
operatorMap["LT"] = function (a,b) { a < b; }
operatorMap["GT"] = function (a,b) { a > b; }

class Runnable {
	constructor(toRun) {
		this.toRun = toRun;
	}
	
	run() {
		this.toRun();
	}
}

class CompositeRunnable {
	constructor() {
		this.runnables = [];
	}
	
	add(toRun) {
		this.runnables.push(toRun);
	}
	
	run() {
		for(var i = 0; i < this.runnables.length; ++i) {
			this.runnables[i].run();
		}
	}
}

class RtmtMimics {
	/**
	* Create a new instance of the RtmtMimics object. Such object can be used to instantiate
	* individual MimicController objects, for each mimic to be placed in the document.
	* @constructor
	*/
	constructor(htmlDocument) {
		this.htmlDocument = htmlDocument;
	}
	
	newMimic(svgPath, svgDomLocation, objPropertyNames) {
		return new MimicController(this.htmlDocument, svgPath, svgDomLocation, objPropertyNames);
	}
}

class MimicController {

	constructor(htmlDocument, svgPath, svgDomLocation, objPropertyNames) {
		this.htmlDocument = htmlDocument;
		this.svgPath = svgPath;
		this.svgDomLocation = svgDomLocation;
		this.objPropertyNames = objPropertyNames;
		this.svgDocument = null;
		this.svgDocumentElement = null;
		this.path2processors = [];
		this.initialised = false;
		this.bindings = [];
	}
	
	async initialise() {
		if(!this.initialised) {
			console.log("Fetching " + this.svgPath + " ..." ); 
			let response = await fetch(this.svgPath);
			console.log("Status:      " + response.status); // 200
			console.log("Status Text: " + response.statusText); // OK

			if (response.status === 200) {
				let data = await response.text();	
				// parse and add SVG to document
				var parser = new DOMParser();
				this.svgDocument = parser.parseFromString(data, "image/svg+xml");
				this.svgDocumentElement = this.svgDocument.documentElement;
				this.svgDomLocation.appendChild(this.svgDocumentElement);
				// initialise the mimic dynamics, by navigating all the SVG objects having a data-rtmt-binding-id attribute
				this.navigateSvg();
				// mark as initialised
				this.initialised = true;
			}
		}
    }
	
	dispose() {
		if(this.initialised) {
			this.svgDocumentElement.remove();
			this.svgDocument = null;
			this.svgDocumentElement = null;
			this.initialised = false;
		}
	}
	
	navigateSvg() {
		var nsResolver = this.htmlDocument.createNSResolver( this.svgDomLocation.ownerDocument == null ? this.svgDomLocation.documentElement : this.svgDomLocation.ownerDocument.documentElement );
		var xpathResult = this.htmlDocument.evaluate( "//*[@" + DATA_RTMT_BINDING_ID + "]", this.svgDomLocation, nsResolver, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null );
		
		try {
			var thisNode = xpathResult.iterateNext();

			while (thisNode) {
				// get the parameter binding
				var parameterBinding = thisNode.attributes[DATA_RTMT_BINDING_ID].value;
				console.log( "Name " + thisNode.nodeName + " " + thisNode.textContent + " - Binding: " + parameterBinding );
				// add to the list of known bindings
				this.bindings.push(parameterBinding);
				// create a SvgElementProcessor for this node and initialise it 
				var processor = new SvgElementProcessor(thisNode, this.objPropertyNames);
				processor.initialise();
				// add the processor to a map for the specific parameter
				var listOfProcs = this.path2processors[parameterBinding];
				if( listOfProcs == null) {
					listOfProcs = [];
					this.path2processors[parameterBinding] = listOfProcs;
				}
				listOfProcs.push(processor);
				// next node
				thisNode = xpathResult.iterateNext();
			}
			
			// remove the duplicates from the bindings list
			this.bindings = this.bindings.filter(onlyUnique);
		}
		catch (e) {
			alert( 'Error: ' + e );
		}
	}
	
	getBindings() {
		return this.bindings;
	}
	
	/**
	* Update the mimic reflecting the status of the provided associative array of objects.
	* The keys are the parameter IDs, as reported in the bindings. The value of each key is a 
	* Javascript object (or associative array), whose properties are used for the evaluation of
	* conditions and expressions of the defined processors.
	*/
	async update(objMap) {
		if(this.initialised) {
			var toBeApplied = [];
			var keys = Object.keys(objMap);
			// iterate on each update object
			for(var i = 0; i < keys.length; ++i) {
				var obj = objMap[keys[i]];
				// retrieve the list of element processors related to the detected binding
				var processors = this.path2processors[keys[i]];
				if(processors != null) {
					// for each element processor, compute the update to be applied
					for(var j = 0; j < processors.length; ++j) {
						var theUpdate = processors[j].buildUpdate(obj);
						toBeApplied.push(theUpdate);
					}
				}
			}
			// apply the updates
			toBeApplied.forEach(a => a.run());
		}
	}
}

class SvgElementProcessor {
	constructor(svgElement, objPropertyNames) {
		this.svgElement = svgElement;
		this.objPropertyNames = objPropertyNames;
		this.type2processorList = [];
	}
	
	initialise() {
		console.log("Initialising " + this.svgElement);
		// iterate on attributes of the element and create the 
		// necessary attribute processors
		for (const a of this.svgElement.attributes) {
			console.log("Attribute " + a.name + " found");
			if(a.name.startsWith(FILL_PREFIX)) {
				var processor = new FillAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(FILL_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(STROKE_PREFIX)) {
				var processor = new StrokeAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(STROKE_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(STROKE_WIDTH_PREFIX)) {
				var processor = new StrokeWidthAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(STROKE_WIDTH_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(VISIBILITY_PREFIX)) {
				var processor = new VisibilityAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(VISIBILITY_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(TEXT_PREFIX)) {
				var processor = new TextNodeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(TEXT_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(TRANSFORM_PREFIX)) {
				var processor = new TransformAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(TRANSFORM_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(BLINK_PREFIX)) {
				var processor = new BlinkNodeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(BLINK_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(ROTATE_PREFIX)) {
				var processor = new RotateNodeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(ROTATE_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(WIDTH_PREFIX)) {
				var processor = new WidthAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(WIDTH_PREFIX, processor);
				continue;
			}
			if(a.name.startsWith(HEIGHT_PREFIX)) {
				var processor = new HeightAttributeProcessor(this.svgElement, a.name, a.value, this.objPropertyNames);
				this.addToProcessors(HEIGHT_PREFIX, processor);
				continue;
			}
		}
		// sort each list in this.type2processor map
		this.type2processorList.forEach(value => {
			value.sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0));
		})
	}
	
	addToProcessors(type, processor) {
		var listOfProcs = this.type2processorList[type];
		if( listOfProcs == null) {
			listOfProcs = [];
			this.type2processorList[type] = listOfProcs;
		}
		listOfProcs.push(processor);
	}
	
	buildUpdate(obj) {
		var compRunnable = new CompositeRunnable();
		for(var i = 0; i < SUPPORTED_SVG_ACTIONS.length; ++i) {
			var procList = this.type2processorList[SUPPORTED_SVG_ACTIONS[i]];
			if(procList != null) {
				// iterate on the attribute processors and derive the action to be done
				for(var j = 0; j < procList.length; ++j) {
					var proc = procList[j];
					if(proc.test(obj)) {
						compRunnable.add(proc.buildUpdate(obj));
						// stop this property
						break;
					}
				}
			}
		}
		return compRunnable;
	}
}

class SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		this.svgElement = svgElement;
		this.name = name;
		this.value = value;
		this.objPropertyNames = objPropertyNames;
		this.condition = null;
		this.expression = null;
		// parse condition expression text
        this.parseConditionExpression(value);
	}
	
	parseConditionExpression(conditionExpressionText) {
		conditionExpressionText = conditionExpressionText.trim();
		console.log("Parsing condition expression text: " + conditionExpressionText);
        if(conditionExpressionText.startsWith(":=")) {
            // condition -> true
            this.condition = new AlwaysTrueEvaluator();
            // expression to parse
            this.parseExpression(conditionExpressionText.substring(2));
        } else {
            var idx = conditionExpressionText.indexOf(":=");
            this.condition = this.parseCondition(conditionExpressionText.substring(0, idx));
            this.parseExpression(conditionExpressionText.substring(idx + 2));
        }
	}
	
	parseCondition(conditionText) {
        // a condition is composed by 3 fields, separated by space: 
		// <reference>' '<operator>' '<reference value>'
		conditionText = conditionText.trim();
        var parts = conditionText.split(" ", -1);
        var referenceExtractor = this.parseReference(parts[0].trim(), true);
        var referenceValueExtractor = this.parseReference(parts[2].trim(), false);
		var operator = operatorMap[parts[1].trim()];
        return new ConditionEvaluator(referenceExtractor, operator, referenceValueExtractor);
    }
	
	parseReference(val, onlyParameterDataRefs) {
        if(val.startsWith('$')) {
            return new MemberExtractor(val.substring(1, val.length));
        } else if(onlyParameterDataRefs) {
            throw "Cannot parse '" + val + "' as reference";
        } else {
            return new FixedExtractor(val);
        }
    }
	
	parseExpression(expressionText) {
        expressionText = expressionText.trim();
		this.expression = new ExpressionFunction(expressionText, this.objPropertyNames);
    }
	
	test(obj) {
		return this.condition.test(obj);
	}
}

/******************************************************************************************
 * Attribute/node processors
 ******************************************************************************************/
 
class AttributeValueApplier extends Runnable {
	constructor(svgElement, attributeName, attributeValue) {
		super(null);
		this.svgElement = svgElement;
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}
	
	run() {
		if(this.attributeValue == null) {
			this.svgElement.removeAttribute(this.attributeName);
		} else {
			this.svgElement.setAttribute(this.attributeName, this.attributeValue);
		}
	}
}

class FillAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "fill", toApply);
	}
}

class StrokeAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "stroke", toApply);
	}
}

class StrokeWidthAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "stroke-width", toApply);
	}
}

class WidthAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "width", toApply);
	}
}

class HeightWidthAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "height", toApply);
	}
}

class TransformAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "transform", toApply);
	}
}

class VisibilityAttributeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new AttributeValueApplier(this.svgElement, "visibility", toApply);
	}
}

class TextNodeProcessor extends SvgAttributeProcessor {
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		return new TextNodeApplier(this.svgElement, toApply);
	}
}

class TextNodeApplier extends Runnable {
	constructor(svgElement, elementText) {
		super(null);
		this.svgElement = svgElement;
		this.elementText = elementText;
	}
	
	run() {
		// Get the first text node (if exist) as child of the provided element
		var theTextNode = null;
		for(var i = 0; i < this.svgElement.childNodes.length; ++i) {
			var child = this.svgElement.childNodes.item(i);
			if(child.nodeType == Node.TEXT_NODE) {
				theTextNode = child;
				break;
			}				
		}
		// At this stage, theTextNode is either null or with the first TextNode. Let's check what to do.
		if(this.elementText == null && theTextNode != null) {
			// Remove the text node
			this.svgElement.removeChild(theTextNode);
		} else if(this.elementText != null && theTextNode == null) {
			// Add a text node
			theTextNode = this.svgElement.ownerDocument.createTextNode(this.elementText);
			this.svgElement.prepend(theTextNode);
		} else if(this.elementText != null && theTextNode != null) {
			// Replace value
			theTextNode.textContent = this.elementText;
		}
	}
}

class BlinkNodeProcessor extends SvgAttributeProcessor {
	// TODO: cache the BlinkNodeApplier operation to avoid the search of the animate node?
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		if(toApply == NO_BLINK) {
			toApply = null;
		}
		return new BlinkNodeApplier(this.svgElement, deriveStringColour(toApply));
	}
}

class BlinkNodeApplier extends Runnable {
	constructor(svgElement, elementText) {
		super(null);
		this.svgElement = svgElement;
		this.elementText = elementText;
	}
	
	run() {
		// Get the first animate element (if exist) as child of the provided element
		var theAnimateNode = null;
		for(var i = 0; i < this.svgElement.children.length; ++i) {
			var child = this.svgElement.children.item(i);
			if(child.tagName == "animate") {
				theAnimateNode = child;
				break;
			}				
		}
		// At this stage, theAnimateNode is either null or with the first animate element. Let's check what to do.
		if(this.elementText == null && theAnimateNode != null) {
			// Remove the animate node
			this.svgElement.removeChild(theAnimateNode);
		} else if(this.elementText != null && theAnimateNode == null) {
			// Add a animate element
			theAnimateNode = this.svgElement.ownerDocument.createElementNS("http://www.w3.org/2000/svg", "animate");
			theAnimateNode.setAttribute("attributeType", "XML");
			theAnimateNode.setAttribute("attributeName", "fill");
			theAnimateNode.setAttribute("dur", "1.0s");
			theAnimateNode.setAttribute("repeatCount", "indefinite");
			theAnimateNode.setAttribute("values", this.elementText);
			this.svgElement.prepend(theAnimateNode);
		} else if(this.elementText != null && theAnimateNode != null) {
			// Replace values in node
			theAnimateNode.setAttribute("values", this.elementText);
		}
	}
}

class RotateNodeProcessor extends SvgAttributeProcessor {
	// TODO: cache the BlinkNodeApplier operation to avoid the search of the animate node?
	constructor(svgElement, name, value, objPropertyNames) {
		super(svgElement, name, value, objPropertyNames);
	}
	
	buildUpdate(obj) {
		var toApply = this.expression.compute(obj);
		if(toApply == NO_ROTATE) {
			toApply = null;
		}
		return new RotateNodeApplier(this.svgElement, toApply);
	}
}

class RotateNodeApplier extends Runnable {
	constructor(svgElement, elementText) {
		super(null);
		this.svgElement = svgElement;
		this.elementText = elementText;
		if(this.elementText != null) {
			// parse: duration centerX, centerY
			this.elementText = this.elementText.trim().split(" ", -1);
		}
	}
	
	run() {
		// Get the first animate element (if exist) as child of the provided element
		var theAnimateNode = null;
		for(var i = 0; i < this.svgElement.children.length; ++i) {
			var child = this.svgElement.children.item(i);
			if(child.tagName == "animateTransform") {
				theAnimateNode = child;
				break;
			}				
		}
		// At this stage, theAnimateNode is either null or with the first animate element. Let's check what to do.
		if(this.elementText == null && theAnimateNode != null) {
			// Remove the animate node
			this.svgElement.removeChild(theAnimateNode);
		} else if(this.elementText != null && theAnimateNode == null) {
			// Add a animate element
			theAnimateNode = this.svgElement.ownerDocument.createElementNS("http://www.w3.org/2000/svg", "animateTransform");
			theAnimateNode.setAttribute("attributeType", "XML");
			theAnimateNode.setAttribute("attributeName", "transform");
			theAnimateNode.setAttribute("type", "rotate");
			theAnimateNode.setAttribute("repeatCount", "indefinite");
			
			theAnimateNode.setAttribute("from", "0 " + this.elementText[1] + " " + this.elementText[2]);
			theAnimateNode.setAttribute("to", "360 " + this.elementText[1] + " " + this.elementText[2]);
			theAnimateNode.setAttribute("dur", this.elementText[0] + "ms");
			
			this.svgElement.prepend(theAnimateNode);
		} else if(this.elementText != null && theAnimateNode != null) {
			// Replace values in node
			theAnimateNode.setAttribute("from", "0 " + this.elementText[1] + " " + this.elementText[2]);
			theAnimateNode.setAttribute("to", "360 " + this.elementText[1] + " " + this.elementText[2]);
			theAnimateNode.setAttribute("dur", this.elementText[0] + "ms");
		}
	}
}

/******************************************************************************************
 * Condition/Expression objects
 ******************************************************************************************/

class ExpressionFunction {
	constructor(expressionText, objPropertyNames) {
		this.expressionText = expressionText;
		this.objPropertyNames = objPropertyNames;
	}
	
	compute(obj) {
		var newText = this.expressionText;
		for(var i = 0; i < this.objPropertyNames.length; ++i) {
			newText = newText.replace("$" + this.objPropertyNames[i], obj[this.objPropertyNames[i]]);
		}
		return newText;
	}
}

class ConditionEvaluator {
	
	constructor(referenceExtractor, operator, referenceValueExtractor) {
		this.referenceExtractor = referenceExtractor;
		this.operator = operator;
		this.referenceValueExtractor = referenceValueExtractor;
	}
	
	test(obj) {
		var o1 = this.referenceExtractor.extract(obj);
		var o2 = this.referenceValueExtractor.extract(obj);
		return this.operator(o1, o2);
	}
}

class AlwaysTrueEvaluator {
	test(obj) {
		return true;
	}
}

class MemberExtractor {
	constructor(memberName) {
		this.memberName = memberName;
	}
	
	extract(obj) {
		return obj[this.memberName];
	}
}

class FixedExtractor {
	constructor(valueText) {
		this.initialised = false;
		this.returnValue = null;
		this.valueText = valueText;
	}
	
	extract(obj) {
		if(!this.initialised) {
			if(this.valueText == null || this.valueText == NULL_VALUE) {
				this.returnValue = null;
			} else {
				this.returnValue = parseTextValue(this.valueText);
			}
			this.initialised = true;
		}
		return this.returnValue;
	}
}

/******************************************************************************************
 * Utility functions
 ******************************************************************************************/

// support strings, boolean, number, date
function parseTextValue (s) {
	if(s == null) {
		return null;
	}
	if(s == "") {
		return s;
	}
	// string ends with Z and contains T, -, : -> try date, if you fail it is a string
	if(s.endsWith("Z") && s.contains("T") && s.contains(":") && s.contains("-")) {
		try {
			return Date.parse(s);
		} catch (e) {
			return s;
		}
	}
	// if string is equal to true or false -> boolean
	if(s == "true") {
		return true;
	}
	if(s == "false") {
		return false;
	}
	// at this stage, try with float.
	try {
		if(isNaN(s)) { 
			return s;
		} else {
			return parseFloat(s);
		}
	} catch (e) {
		// Skip
	}
	// if really all fails, it is a string.
	return s;
}


// thanks to SO: https://stackoverflow.com/a/14438954
function onlyUnique(value, index, self) {
  return self.indexOf(value) === index;
}

function deriveStringColour(fill) {
	if(fill == null) {
		return null;
	}
	var baseColor = parseColor(fill);
	// Resulting string is baseColor/2, baseColor/2, baseColor, baseColor/2
	var half = [ baseColor[0]/2, baseColor[1]/2, baseColor[2]/2 ];
	return colorToString(baseColor) + ";" + colorToString(baseColor) + ";" + colorToString(half) + ";" + colorToString(baseColor);
}

function colorToString(c) {
	return "#" + hex(c[0]) + hex(c[1]) + hex(c[2]) + "FF";
}

function hex(v) {
	var vv = Math.round(v).toString(16);
	return vv.length == 2 ? vv : "0" + vv;
}

// thanks to SO: https://stackoverflow.com/a/11068286
function parseColor(input) {
    var div = document.createElement('div'), m;
    div.style.color = input;
	var computedStyle = div.style;
    m = computedStyle.color.match(/^rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/i);
    if(m) {
		return [m[1],m[2],m[3]];
	}
    else throw new Error("Colour "+input+" could not be parsed.");
}