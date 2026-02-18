"""
Python classes representing the A2UI Protocol v0.8 specification.

See: https://a2ui.org/specification/v0.8-a2ui/
"""

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Union

@dataclass
class BoundValue:
    """
    A value that can be a literal or bound to the data model.
    Must contain at least one of: literalString, literalNumber, literalBoolean, literalArray, path.
    """

    literal_string: Optional[str] = None
    literal_number: Optional[Union[int, float]] = None
    literal_boolean: Optional[bool] = None
    literal_array: Optional[List[Any]] = None
    path: Optional[str] = None

    def is_literal_only(self) -> bool:
        """True if only literal value(s) are set (no path)."""
        return self.path is None and (
            self.literal_string is not None
            or self.literal_number is not None
            or self.literal_boolean is not None
            or self.literal_array is not None
        )

    def is_path_only(self) -> bool:
        """True if only path is set."""
        return self.path is not None and not any(
            [
                self.literal_string is not None,
                self.literal_number is not None,
                self.literal_boolean is not None,
                self.literal_array is not None,
            ]
        )

    def is_init_shorthand(self) -> bool:
        """True if both path and a literal are set (initialization shorthand)."""
        return self.path is not None and (
            self.literal_string is not None
            or self.literal_number is not None
            or self.literal_boolean is not None
            or self.literal_array is not None
        )


@dataclass
class ExplicitList:
    """Ordered list of component IDs that are direct children."""

    ids: List[str]


@dataclass
class ContainerTemplate:
    """Template for rendering dynamic lists of children from a data-bound list."""

    data_binding: str  # DataPath, e.g. "/user/posts"
    component_id: str  # ID of the component to use as template for each item


@dataclass
class ContainerChildren:
    """Exactly one of explicit_list or template must be set."""

    explicit_list: Optional[ExplicitList] = None
    template: Optional[ContainerTemplate] = None


@dataclass
class ActionContextEntry:
    """Key-value entry in action.context; value is a BoundValue."""

    key: str
    value: BoundValue


@dataclass
class Action:
    """Action definition on a component (e.g. Button)."""

    name: str
    context: List[ActionContextEntry] = field(default_factory=list)



# Component instance: id + component (one key = type name, value = props dict)
@dataclass
class ComponentInstance:
    """
    A single component in the flat adjacency list.
    component_type and component_props are the unwrapped form of the wire 'component' object.
    """

    id: str
    component_type: str  # e.g. "Text", "Column", "Button"
    component_props: Dict[str, Any] = field(default_factory=dict)

@dataclass
class DataModelEntry:
    """One entry in dataModelUpdate.contents (adjacency list)."""

    key: str
    value_string: Optional[str] = None
    value_number: Optional[Union[int, float]] = None
    value_boolean: Optional[bool] = None
    value_map: Optional[List["DataModelEntry"]] = None

    def value(self) -> Any:
        """Return the single set value."""
        if self.value_string is not None:
            return self.value_string
        if self.value_number is not None:
            return self.value_number
        if self.value_boolean is not None:
            return self.value_boolean
        if self.value_map is not None:
            return _adjacency_list_to_dict(self.value_map)
        raise ValueError(f"DataModelEntry '{self.key}' has no value set")


def _adjacency_list_to_dict(entries: List[DataModelEntry]) -> Dict[str, Any]:
    """Convert adjacency list of DataModelEntry to nested dict."""
    return {e.key: e.value() for e in entries}


@dataclass
class SurfaceUpdate:
    """surfaceUpdate: list of component definitions for a surface."""

    surface_id: Optional[str] = None
    components: List[ComponentInstance] = field(default_factory=list)


@dataclass
class DataModelUpdate:
    """dataModelUpdate: update the data model at path with contents."""

    surface_id: Optional[str] = None
    path: Optional[str] = None  # e.g. "/user/name"; omitted = root
    contents: List[DataModelEntry] = field(default_factory=list)


@dataclass
class BeginRendering:
    """beginRendering: signal to render; root component ID and optional catalog."""

    root: str
    catalog_id: Optional[str] = None
    surface_id: Optional[str] = None


@dataclass
class DeleteSurface:
    """deleteSurface: remove a surface and its contents."""

    surface_id: Optional[str] = None


# Union of server-to-client message payloads
ServerToClientMessage = Union[SurfaceUpdate, DataModelUpdate, BeginRendering, DeleteSurface]


@dataclass
class UserAction:
    """userAction: reported when user interacts with a component that has an action."""

    name: str
    surface_id: str
    source_component_id: str
    timestamp: str  # ISO 8601
    context: Dict[str, Any]


@dataclass
class A2UISurface:
    """
    Aggregated state for one surface after processing messages:
    component map (id -> ComponentInstance), data model (nested dict), root id.
    """

    surface_id: Optional[str] = None
    components: Dict[str, ComponentInstance] = field(default_factory=dict)
    data_model: Dict[str, Any] = field(default_factory=dict)
    root_id: Optional[str] = None
    catalog_id: Optional[str] = None

    def get_root_component(self) -> Optional[ComponentInstance]:
        if self.root_id is None:
            return None
        return self.components.get(self.root_id)


def parse_bound_value(obj: Dict[str, Any]) -> BoundValue:
    """Parse a BoundValue from wire format (e.g. {"literalString": "Hi"} or {"path": "/x"})."""
    return BoundValue(
        literal_string=obj.get("literalString"),
        literal_number=obj.get("literalNumber"),
        literal_boolean=obj.get("literalBoolean"),
        literal_array=obj.get("literalArray"),
        path=obj.get("path"),
    )


def parse_container_children(obj: Dict[str, Any]) -> ContainerChildren:
    """Parse children from wire format: either explicitList or template."""
    if "explicitList" in obj:
        return ContainerChildren(explicit_list=ExplicitList(ids=obj["explicitList"]))
    if "template" in obj:
        t = obj["template"]
        return ContainerChildren(
            template=ContainerTemplate(
                data_binding=t["dataBinding"],
                component_id=t["componentId"],
            )
        )
    raise ValueError("children must contain exactly one of explicitList or template")


def parse_component_instance(wire: Dict[str, Any]) -> ComponentInstance:
    """Parse one entry from surfaceUpdate.components."""
    cid = wire["id"]
    comp = wire["component"]
    if not isinstance(comp, dict) or len(comp) != 1:
        raise ValueError("component must be an object with exactly one key (type name)")
    comp_type = next(iter(comp))
    comp_props = comp[comp_type]
    return ComponentInstance(id=cid, component_type=comp_type, component_props=comp_props or {})


def build_surface_from_messages(
    messages: List[ServerToClientMessage],
    surface_id: Optional[str] = None,
) -> A2UISurface:
    """
    Apply a sequence of server-to-client messages and return the aggregated
    state for the given surface_id (or the first surface mentioned).
    """
    surf = A2UISurface(surface_id=surface_id)
    for msg in messages:
        if isinstance(msg, SurfaceUpdate):
            sid = msg.surface_id or surf.surface_id
            if sid is not None and surf.surface_id is not None and sid != surf.surface_id:
                continue
            if msg.surface_id is not None:
                surf.surface_id = msg.surface_id
            for c in msg.components:
                surf.components[c.id] = c
        elif isinstance(msg, DataModelUpdate):
            sid = msg.surface_id or surf.surface_id
            if sid is not None and surf.surface_id is not None and sid != surf.surface_id:
                continue
            if msg.surface_id is not None:
                surf.surface_id = msg.surface_id
            path = (msg.path or "").strip("/") or None
            merged = _adjacency_list_to_dict(msg.contents)
            if path:
                parts = path.split("/")
                cur: Dict[str, Any] = surf.data_model
                for p in parts[:-1]:
                    if p not in cur or not isinstance(cur[p], dict):
                        cur[p] = {}
                    cur = cur[p]
                cur[parts[-1]] = merged
            else:
                _deep_merge(surf.data_model, merged)
        elif isinstance(msg, BeginRendering):
            sid = msg.surface_id or surf.surface_id
            if sid is not None and surf.surface_id is not None and sid != surf.surface_id:
                continue
            if msg.surface_id is not None:
                surf.surface_id = msg.surface_id
            surf.root_id = msg.root
            if msg.catalog_id is not None:
                surf.catalog_id = msg.catalog_id
    return surf


def _deep_merge(base: Dict[str, Any], other: Dict[str, Any]) -> None:
    """Merge other into base in place."""
    for k, v in other.items():
        if k in base and isinstance(base[k], dict) and isinstance(v, dict):
            _deep_merge(base[k], v)
        else:
            base[k] = v
