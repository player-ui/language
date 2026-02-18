"""Tests for A2UI to Player converter."""

import json
import pytest

from ..models import (
    A2UISurface,
    BeginRendering,
    ComponentInstance,
    ExplicitList,
    SurfaceUpdate,
    build_surface_from_messages,
    parse_component_instance,
)
from ..converter import (
    A2UIToPlayerError,
    _a2ui_path_to_player_path,
    _player_binding_string,
    surface_to_player_flow,
)


class TestPathConversion:
    """A2UI paths use '/'; Player uses '.' and {{}} for binding attributes."""

    def test_a2ui_path_to_player_path(self):
        assert _a2ui_path_to_player_path("/flight/airline") == "flight.airline"
        assert _a2ui_path_to_player_path("/flight/departure/time") == "flight.departure.time"
        assert _a2ui_path_to_player_path("flight/airline") == "flight.airline"
        assert _a2ui_path_to_player_path("/user/name") == "user.name"

    def test_player_binding_string(self):
        assert _player_binding_string("flight.airline") == "{{flight.airline}}"

    def test_path_in_text_converts_to_player_format(self):
        surface = A2UISurface(
            components={
                "root": ComponentInstance(
                    "root",
                    "Text",
                    {"text": {"path": "/a/b/c"}},
                ),
            },
            root_id="root",
        )
        flow = surface_to_player_flow(surface, flow_id="path-test")
        flow_json = json.loads(flow.serialize())
        view = flow_json["views"][0]
        assert view["text"] == "{{a.b.c}}"


def _surface_with_column_and_text():
    """Minimal surface: Column with one Text child."""
    return A2UISurface(
        surface_id="main",
        components={
            "root": ComponentInstance(
                "root",
                "Column",
                {"children": ExplicitList(ids=["txt1"])},
            ),
            "txt1": ComponentInstance(
                "txt1",
                "Text",
                {"text": {"literalString": "Hello"}},
            ),
        },
        data_model={},
        root_id="root",
    )


class TestSurfaceToPlayerFlow:
    def test_minimal_surface_produces_flow(self):
        surface = _surface_with_column_and_text()
        flow = surface_to_player_flow(surface, flow_id="test-flow")
        assert flow.id == "test-flow"
        assert len(flow.views) == 1
        assert flow.views[0].id == "root"
        assert flow.views[0].type == "Column"
        assert flow.navigation.begin == "main"
        assert flow.data is None or flow.data == {}

    def test_missing_root_raises(self):
        surface = A2UISurface(
            components={"root": ComponentInstance("root", "Text", {})},
            root_id=None,
        )
        with pytest.raises(A2UIToPlayerError, match="root_id"):
            surface_to_player_flow(surface)

    #def test_template_without_output_path_raises(self):
    #    from ..models import ContainerTemplate
    #    surface = A2UISurface(
    #        surface_id="main",
    #        components={
    #            "root": ComponentInstance(
    #                "root",
    #                "Column",
    #                {"children": ContainerTemplate(data_binding="/items", component_id="item")},
    #            ),
    #            "item": ComponentInstance("item", "Text", {"text": {"path": "/title"}}),
    #        },
    #        root_id="root",
    #    )
    #    with pytest.raises(A2UIToPlayerError, match="template_output_path|output"):
    #        surface_to_player_flow(surface)

    #def test_template_with_output_path_succeeds(self):
    #    from ..models import ContainerTemplate
    #    surface = A2UISurface(
    #        surface_id="main",
    #        components={
    #            "root": ComponentInstance(
    #                "root",
    #                "Column",
    #                {"children": ContainerTemplate(data_binding="/items", component_id="item")},
    #            ),
    #            "item": ComponentInstance("item", "Text", {"text": {"path": "/title"}}),
    #        },
    #        root_id="root",
    #    )
    #    flow = surface_to_player_flow(
    #        surface,
    #        template_output_path="/items",
    #    )
    #    assert flow.id == "a2ui-flow"
    #    assert len(flow.views) == 1


# A2UI JSON: flight info card (surfaceUpdate.components array)
A2UI_FLIGHT_CARD_JSON = [
    {"id": "root", "component": {"Card": {"child": "flightInfoColumn"}}},
    {
        "id": "flightInfoColumn",
        "component": {
            "Column": {
                "children": {
                    "explicitList": [
                        "headerRow",
                        "statusText",
                        "divider1",
                        "departureRow",
                        "arrivalRow",
                        "divider2",
                        "gateTerminalRow",
                    ]
                },
                "distribution": "start",
                "alignment": "start",
            }
        },
    },
    {
        "id": "headerRow",
        "component": {
            "Row": {
                "children": {"explicitList": ["airlineText", "flightNumberText"]},
                "distribution": "spaceBetween",
                "alignment": "center",
            }
        },
    },
    {
        "id": "airlineText",
        "component": {"Text": {"text": {"path": "/flight/airline"}, "usageHint": "h3"}},
    },
    {
        "id": "flightNumberText",
        "component": {
            "Text": {"text": {"path": "/flight/flightNumber"}, "usageHint": "h3"}
        },
    },
    {
        "id": "statusText",
        "component": {
            "Text": {"text": {"literalString": "Status: "}, "usageHint": "body"}
        },
    },
    {
        "id": "statusValueText",
        "component": {
            "Text": {"text": {"path": "/flight/status"}, "usageHint": "body"}
        },
    },
    {"id": "divider1", "component": {"Divider": {"axis": "horizontal"}}},
    {
        "id": "departureRow",
        "component": {
            "Row": {
                "children": {
                    "explicitList": [
                        "departureLabel",
                        "departureAirportText",
                        "departureTimeText",
                    ]
                },
                "distribution": "spaceBetween",
                "alignment": "center",
            }
        },
    },
    {
        "id": "departureLabel",
        "component": {
            "Text": {
                "text": {"literalString": "Departure"},
                "usageHint": "body",
            }
        },
    },
    {
        "id": "departureAirportText",
        "component": {
            "Text": {
                "text": {"path": "/flight/departure/airport"},
                "usageHint": "body",
            }
        },
    },
    {
        "id": "departureTimeText",
        "component": {
            "Text": {
                "text": {"path": "/flight/departure/time"},
                "usageHint": "body",
            }
        },
    },
    {
        "id": "arrivalRow",
        "component": {
            "Row": {
                "children": {
                    "explicitList": [
                        "arrivalLabel",
                        "arrivalAirportText",
                        "arrivalTimeText",
                    ]
                },
                "distribution": "spaceBetween",
                "alignment": "center",
            }
        },
    },
    {
        "id": "arrivalLabel",
        "component": {
            "Text": {"text": {"literalString": "Arrival"}, "usageHint": "body"}
        },
    },
    {
        "id": "arrivalAirportText",
        "component": {
            "Text": {
                "text": {"path": "/flight/arrival/airport"},
                "usageHint": "body",
            }
        },
    },
    {
        "id": "arrivalTimeText",
        "component": {
            "Text": {
                "text": {"path": "/flight/arrival/time"},
                "usageHint": "body",
            }
        },
    },
    {"id": "divider2", "component": {"Divider": {"axis": "horizontal"}}},
    {
        "id": "gateTerminalRow",
        "component": {
            "Row": {
                "children": {
                    "explicitList": [
                        "gateLabel",
                        "gateText",
                        "terminalLabel",
                        "terminalText",
                    ]
                },
                "distribution": "spaceBetween",
                "alignment": "center",
            }
        },
    },
    {
        "id": "gateLabel",
        "component": {
            "Text": {"text": {"literalString": "Gate"}, "usageHint": "caption"}
        },
    },
    {
        "id": "gateText",
        "component": {
            "Text": {"text": {"path": "/flight/gate"}, "usageHint": "caption"}
        },
    },
    {
        "id": "terminalLabel",
        "component": {
            "Text": {"text": {"literalString": "Terminal"}, "usageHint": "caption"}
        },
    },
    {
        "id": "terminalText",
        "component": {
            "Text": {
                "text": {"path": "/flight/terminal"},
                "usageHint": "caption",
            }
        },
    },
]


class TestA2UIFlightCardToPlayerFlow:
    """Convert the flight card A2UI JSON to Player Flow and assert on the output JSON."""

    def test_convert_flight_card_a2ui_json_to_player_flow_json(self):
        # Parse A2UI JSON into surfaceUpdate + beginRendering
        components = [parse_component_instance(item) for item in A2UI_FLIGHT_CARD_JSON]
        messages = [
            SurfaceUpdate(components=components),
            BeginRendering(root="root"),
        ]
        surface = build_surface_from_messages(messages)

        # Convert to Player Flow
        flow = surface_to_player_flow(surface, flow_id="flight-card-flow")

        # Serialize Flow to JSON (Player Flow uses Serializable)
        flow_json_str = flow.serialize()
        flow_json = json.loads(flow_json_str)

        # Assert top-level Player Flow structure
        assert flow_json["id"] == "flight-card-flow"
        assert "navigation" in flow_json
        assert flow_json["navigation"]["BEGIN"] == "main"
        assert "views" in flow_json
        assert len(flow_json["views"]) == 1

        view = flow_json["views"][0]
        assert view["id"] == "root"
        assert view["type"] == "Card"
        assert "child" in view  # Card has single child (flightInfoColumn -> Column)

        # Column (flightInfoColumn) has children array
        child = view["child"]
        if isinstance(child, dict) and "asset" in child:
            column_asset = child["asset"]
        else:
            column_asset = child
        assert column_asset["id"] == "flightInfoColumn"
        assert column_asset["type"] == "Column"
        assert "children" in column_asset
        children_ids = [
            c["asset"]["id"] if isinstance(c, dict) and "asset" in c else c.get("id")
            for c in column_asset["children"]
        ]
        assert "headerRow" in children_ids
        assert "departureRow" in children_ids
        assert "arrivalRow" in children_ids
        assert "gateTerminalRow" in children_ids
        assert "statusText" in children_ids
        assert "divider1" in children_ids
        assert "divider2" in children_ids

        # Path bindings preserved (e.g. airline text)
        def find_asset_by_id(node, aid):
            if isinstance(node, dict):
                if node.get("id") == aid:
                    return node
                if "asset" in node:
                    return find_asset_by_id(node["asset"], aid)
                for v in node.values():
                    if isinstance(v, list):
                        for i in v:
                            r = find_asset_by_id(i, aid)
                            if r is not None:
                                return r
                    elif isinstance(v, dict):
                        r = find_asset_by_id(v, aid)
                        if r is not None:
                            return r
            return None

        airline_text = find_asset_by_id(flow_json, "airlineText")
        assert airline_text is not None
        # A2UI paths (/) are converted to Player (.) and wrapped in {{}} when used as attributes
        assert airline_text.get("text") == "{{flight.airline}}"

        status_text = find_asset_by_id(flow_json, "statusText")
        assert status_text is not None
        assert status_text.get("text") == "Status: "

        print(flow_json_str)
