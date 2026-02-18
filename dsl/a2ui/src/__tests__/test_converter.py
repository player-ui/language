"""Tests for A2UI to Player converter."""

import pytest

from ..models import (
    A2UISurface,
    ComponentInstance,
    ExplicitList,
)
from ..converter import (
    A2UIToPlayerError,
    surface_to_player_flow,
)


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
        assert flow.serialize() == ""
        assert flow.data is not None or flow.data == {}

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
    #    print(flow)
    #    assert flow.id == "a2ui-flow"
    #    assert len(flow.views) == 1
