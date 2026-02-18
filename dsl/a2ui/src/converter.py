"""
Converter from A2UI protocol model to Player content standard (Flow, View, Asset, etc.).

Maps semantic elements of the A2UI specification to their equivalents in the Player
specification. Raises A2UIToPlayerError when a construct cannot be mapped.
"""

from typing import Any, Dict, Optional

# Player content standard (dsl/python/src)
from player_lang_dsl.view import (
    Asset,
    AssetWrapper,
    Template as PlayerTemplate,
    View,
)
from player_lang_dsl.flow import Flow
from player_lang_dsl.navigation import (
    Navigation,
    NavigationFlow,
    NavigationFlowEndState,
    NavigationFlowViewState,
)

from .models import (
    A2UISurface,
    BoundValue,
    ComponentInstance,
    ContainerTemplate,
    ExplicitList,
    parse_bound_value,
)


class A2UIToPlayerError(Exception):
    """Raised when an A2UI construct cannot be mapped to Player."""

    pass


def _convert_bound_value_to_player(bv: BoundValue, prop_name: str) -> Any:
    """
    Convert A2UI BoundValue to a form Player can use (binding string or literal).
    """
    if bv.is_literal_only():
        if bv.literal_string is not None:
            return bv.literal_string
        if bv.literal_number is not None:
            return bv.literal_number
        if bv.literal_boolean is not None:
            return bv.literal_boolean
        if bv.literal_array is not None:
            return bv.literal_array
    if bv.is_path_only():
        return bv.path
    if bv.is_init_shorthand():
        raise A2UIToPlayerError(
            f"BoundValue for '{prop_name}' uses path+literal initialization shorthand. "
            "Player has no direct equivalent; initialize the data model separately and use path-only binding."
        )
    raise A2UIToPlayerError(
        f"BoundValue for '{prop_name}' has no literal and no path."
    )


def _convert_props_to_player(props: Dict[str, Any], component_id: str) -> Dict[str, Any]:
    """Convert A2UI component_props to slot values for Player Asset (excluding children)."""
    out: Dict[str, Any] = {}
    for key, val in props.items():
        if val is None:
            continue
        if isinstance(val, BoundValue):
            out[key] = _convert_bound_value_to_player(val, key)
        elif isinstance(val, dict):
            if any(
                k in val
                for k in ("literalString", "path", "literalNumber", "literalBoolean", "literalArray")
            ):
                bv = parse_bound_value(val)
                out[key] = _convert_bound_value_to_player(bv, key)
            elif "explicitList" in val or "template" in val:
                continue
            else:
                out[key] = _convert_nested_prop_to_player(val)
        else:
            out[key] = val
    return out


def _convert_nested_prop_to_player(val: Dict[str, Any]) -> Any:
    """Convert nested structures (e.g. action with context) for Player."""
    if "name" in val:
        result: Dict[str, Any] = {"name": val["name"]}
        if "context" in val:
            ctx = val["context"]
            if isinstance(ctx, list):
                result["context"] = []
                for entry in ctx:
                    if isinstance(entry, dict) and "key" in entry and "value" in entry:
                        v = entry["value"]
                        if isinstance(v, dict) and (
                            "path" in v
                            or "literalString" in v
                            or "literalNumber" in v
                            or "literalBoolean" in v
                            or "literalArray" in v
                        ):
                            bv = parse_bound_value(v)
                            result["context"].append(
                                {"key": entry["key"], "value": _convert_bound_value_to_player(bv, entry["key"])}
                            )
                        else:
                            result["context"].append(entry)
                    else:
                        result["context"].append(entry)
            else:
                result["context"] = ctx
        return result
    return val


def _normalize_component_props(comp: ComponentInstance) -> ComponentInstance:
    """
    Replace raw dicts in component_props with BoundValue, ExplicitList, or ContainerTemplate
    so the rest of the converter can handle them uniformly.
    """
    props: Dict[str, Any] = {}
    for k, v in comp.component_props.items():
        if isinstance(v, dict):
            if any(
                x in v
                for x in ("literalString", "path", "literalNumber", "literalBoolean", "literalArray")
            ):
                props[k] = parse_bound_value(v)
            elif "explicitList" in v:
                props[k] = ExplicitList(ids=v["explicitList"])
            elif "template" in v:
                t = v["template"]
                props[k] = ContainerTemplate(
                    data_binding=t["dataBinding"],
                    component_id=t["componentId"],
                )
            else:
                props[k] = v
        else:
            props[k] = v
    return ComponentInstance(
        id=comp.id,
        component_type=comp.component_type,
        component_props=props,
    )


def _build_asset_tree(
    surface: A2UISurface,
    root_id: str,
    id_to_asset: Dict[str, Asset],
    template_output_path: Optional[str] = None,
) -> Asset:
    """
    Build the Player Asset tree from the flat component map.
    Preserves A2UI component IDs by assigning slots directly (no _setParent).
    """
    comp = surface.components.get(root_id)
    if not comp:
        raise A2UIToPlayerError(f"Root component id '{root_id}' not found in surface components.")
    comp = _normalize_component_props(comp)

    def get_or_create_asset(cid: str) -> Asset:
        if cid in id_to_asset:
            return id_to_asset[cid]
        c = surface.components.get(cid)
        if not c:
            raise A2UIToPlayerError(f"Component id '{cid}' not found.")
        c = _normalize_component_props(c)
        props = _convert_props_to_player(c.component_props, cid)
        asset = Asset(id=cid, type=c.component_type)
        for k, v in props.items():
            asset[k] = v
        id_to_asset[cid] = asset

        # Children: explicitList or template
        children_val = c.component_props.get("children")
        if isinstance(children_val, ExplicitList):
            child_assets = [get_or_create_asset(i) for i in children_val.ids]
            asset["children"] = [AssetWrapper(a) for a in child_assets]
        elif isinstance(children_val, ContainerTemplate):
            #if template_output_path is None:
            #    raise A2UIToPlayerError(
            #        "A2UI container uses 'template' (dynamic list). Player Template requires an 'output' path; "
            #        "A2UI has no equivalent. Pass template_output_path when converting."
            #    )
            #template_asset = get_or_create_asset(children_val.component_id)
            #player_template = PlayerTemplate(isDynamic=True)
            #player_template.withData(children_val.data_binding)
            #player_template.withOutput(template_output_path)
            #player_template.withAsset(AssetWrapper(template_asset))
            #asset["children"] = [AssetWrapper(player_template)]
             raise A2UIToPlayerError("Templates aren't supported yet")
        elif children_val is not None:
            raise A2UIToPlayerError(f"Unsupported children value type: {type(children_val)}")

        # Single child (e.g. Card.child)
        for single_slot in ("child", "content"):
            ref = c.component_props.get(single_slot)
            if isinstance(ref, str):
                child = get_or_create_asset(ref)
                asset[single_slot] = AssetWrapper(child)

        return asset

    return get_or_create_asset(root_id)


def surface_to_player_flow(
    surface: A2UISurface,
    flow_id: str = "a2ui-flow",
    template_output_path: Optional[str] = None,
) -> Flow:
    """
    Convert one A2UI surface to a Player Flow.

    - Surface root component -> single View (Asset tree rooted at that component).
    - Surface data model -> Flow.data.
    - Creates a minimal Navigation: one VIEW state (this view) -> END.

    Raises A2UIToPlayerError when a construct cannot be mapped (e.g. template without
    template_output_path, or path+literal BoundValue).
    """
    root_id = surface.root_id
    if not root_id:
        raise A2UIToPlayerError("Surface has no root_id; need a beginRendering message with root.")

    id_to_asset: Dict[str, Asset] = {}
    root_asset = _build_asset_tree(
        surface,
        root_id,
        id_to_asset,
        template_output_path=template_output_path,
    )

    # Player View is an Asset; use root as the view and copy its slots.
    view = View(id=root_asset.id, type=root_asset.type)
    skip = {"id", "type", "validation", "_parent", "_slot_name", "_slot_index"}
    for key, val in root_asset.__dict__.items():
        if key in skip or key.startswith("_"):
            continue
        view[key] = val

    # Navigation: one flow with VIEW -> END
    view_state = NavigationFlowViewState(
        ref=view.id,
        transitions={"next": "end"},
    )
    end_state = NavigationFlowEndState(outcome="done")
    nav_flow = NavigationFlow(
        start_state="view_state",
        view_state=view_state,
        end=end_state,
    )
    nav = Navigation(BEGIN="main", main=nav_flow)

    return Flow(
        id=flow_id,
        navigation=nav,
        views=[view],
        data=surface.data_model if surface.data_model else None,
    )
